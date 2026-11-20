package com.dozy.features.story

import com.dozy.config.DatabaseConfig
import com.dozy.config.GenerationConfig
import com.dozy.domain.id.OwnerId
import com.dozy.domain.id.StoryId
import com.dozy.features.story.application.GenerationWorker
import com.dozy.features.story.data.FakeCoverGenerator
import com.dozy.features.story.data.database.StoryJobs
import com.dozy.features.story.data.repository.SqliteGenerationRepository
import com.dozy.features.story.domain.model.CoverGenerator
import com.dozy.features.story.domain.model.Execution
import com.dozy.features.story.domain.model.GeneratedText
import com.dozy.features.story.domain.model.Stage
import com.dozy.features.story.domain.model.Submission
import com.dozy.features.story.domain.model.TextGenerator
import com.dozy.features.story.domain.repository.GenerationRepository
import com.dozy.infrastructure.database.SqliteDatabase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.update
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.time.Clock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GenerationWorkerTest {
    private val clock = Clock.systemUTC()
    @TempDir lateinit var directory: Path
    private val options = GenerationConfig(delayMillis = 0, leaseMillis = 300, concurrency = 2)
    private val ownerId = requireNotNull(OwnerId.parse("owner-019a0000-0000-7000-8000-000000000010"))
    private val dbConfig get() = DatabaseConfig(directory.resolve("dozy.db").toString(), 5000)

    @Test fun `long generator renews claim and stages expose no premature result`() = runBlocking {
        SqliteDatabase(dbConfig).use { db ->
            val repository = SqliteGenerationRepository(db, options, clock)
            val job = assertIs<Submission.Accepted>(repository.start(ownerId, requestKey(), "request", command())).job
            val started = CompletableDeferred<Unit>()
            val finish = CompletableDeferred<Unit>()
            val text = TextGenerator {
                assertEquals(Stage.CREATING_TEXT, repository.status(ownerId, job.id)?.stage)
                started.complete(Unit)
                finish.await()
                GeneratedText("Title", "Text")
            }
            val cover = CoverGenerator {
                val status = assertNotNull(repository.status(ownerId, job.id))
                assertEquals(Stage.CREATING_COVER, status.stage)
                assertNull(status.storyId)
                FakeCoverGenerator(options).generate(it)
            }
            val run = async { GenerationWorker(repository, text, cover, options).runOne() }
            started.await()
            delay(450)
            assertNull(repository.claim())
            finish.complete(Unit)
            assertTrue(run.await())
            assertEquals(Stage.READY, repository.status(ownerId, job.id)?.stage)
        }
    }
    @Test fun `cancellation and publication storage failures remain recoverable instead of failed`() = runBlocking {
        SqliteDatabase(dbConfig).use { db ->
            val repository = SqliteGenerationRepository(db, options, clock)
            val job = assertIs<Submission.Accepted>(repository.start(ownerId, requestKey(), "request", command())).job
            val cancelling = GenerationWorker(repository, TextGenerator { throw CancellationException("process stopped") }, FakeCoverGenerator(options), options)
            assertFailsWith<CancellationException> { cancelling.runOne() }
            assertEquals(Stage.CREATING_TEXT, repository.status(ownerId, job.id)?.stage)
            db.write { StoryJobs.update({ StoryJobs.id eq job.id.value }) { it[leaseUntil] = 0 } }
            val failingStorage = object : GenerationRepository by repository {
                override suspend fun publish(execution: Execution, cover: ByteArray): StoryId? = error("Storage temporarily unavailable")
            }
            val text = TextGenerator { GeneratedText("Title", "Text") }
            assertFailsWith<IllegalStateException> { GenerationWorker(failingStorage, text, FakeCoverGenerator(options), options).runOne() }
            val status = assertNotNull(repository.status(ownerId, job.id))
            assertEquals(Stage.CREATING_COVER, status.stage)
            assertNull(status.failure)
            db.write { StoryJobs.update({ StoryJobs.id eq job.id.value }) { it[leaseUntil] = 0 } }
            assertTrue(GenerationWorker(repository, TextGenerator { error("Persisted text must be reused") }, FakeCoverGenerator(options), options).runOne())
            assertEquals(Stage.READY, repository.status(ownerId, job.id)?.stage)
        }
    }
    @Test fun `configured concurrency bounds active generator calls across owners`() = runBlocking {
        SqliteDatabase(dbConfig).use { db ->
            val repository = SqliteGenerationRepository(db, options, clock)
            val jobs = (1..5).map { index ->
                val owner = requireNotNull(OwnerId.parse("owner-019a0000-0000-7000-8000-00000000000$index"))
                owner to assertIs<Submission.Accepted>(repository.start(owner, requestKey(), "request", command())).job
            }
            var active = 0
            var maximum = 0
            val gate = CompletableDeferred<Unit>()
            val text = TextGenerator {
                active++
                maximum = maxOf(maximum, active)
                try { gate.await(); GeneratedText("Title", "Text") } finally { active-- }
            }
            val runners = GenerationWorker(repository, text, FakeCoverGenerator(options), options).start(this)
            try {
                withTimeout(5000) { while (active < 2) delay(10) }
                assertEquals(2, maximum)
                gate.complete(Unit)
                withTimeout(5000) { while (jobs.any { (owner, job) -> repository.status(owner, job.id)?.stage != Stage.READY }) delay(10) }
                assertEquals(2, maximum)
            } finally { runners.forEach { it.cancelAndJoin() } }
        }
    }
}
