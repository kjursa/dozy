package com.dozy.features.story

import com.dozy.config.DatabaseConfig
import com.dozy.config.GenerationConfig
import com.dozy.domain.id.OwnerId
import com.dozy.domain.id.StoryId
import com.dozy.features.story.application.GenerationWorker
import com.dozy.features.story.application.StoryGenerationScheduler
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
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
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

class GenerationWorkerTest {
    private val clock = Clock.systemUTC()

    @TempDir lateinit var directory: Path
    private val options = GenerationConfig(delayMillis = 0, leaseMillis = 300, concurrency = 2)
    private val ownerId = requireNotNull(OwnerId.parse("owner-019a0000-0000-7000-8000-000000000010"))
    private val dbConfig get() = DatabaseConfig(directory.resolve("dozy.db").toString(), 5000)

    @Test fun `long generator renews claim and stages expose no premature result`() =
        runBlocking {
            SqliteDatabase(dbConfig).use { db ->
                val repository = SqliteGenerationRepository(db, options, clock)
                val job = assertIs<Submission.Accepted>(repository.start(ownerId, requestKey(), "request", command())).job
                val started = CompletableDeferred<Unit>()
                val finish = CompletableDeferred<Unit>()
                val text =
                    TextGenerator {
                        assertEquals(Stage.CREATING_TEXT, repository.status(ownerId, job.id)?.stage)
                        started.complete(Unit)
                        finish.await()
                        GeneratedText("Title", "Text")
                    }
                val cover =
                    CoverGenerator {
                        val status = assertNotNull(repository.status(ownerId, job.id))
                        assertEquals(Stage.CREATING_COVER, status.stage)
                        assertNull(status.storyId)
                        FakeCoverGenerator(options).generate(it)
                    }
                val run = async { GenerationWorker(repository, text, cover, options).run(assertNotNull(repository.claim())) }
                started.await()
                delay(450)
                assertNull(repository.claim())
                finish.complete(Unit)
                run.await()
                assertEquals(Stage.READY, repository.status(ownerId, job.id)?.stage)
            }
        }

    @Test fun `cancellation and publication storage failures remain recoverable instead of failed`() =
        runBlocking {
            SqliteDatabase(dbConfig).use { db ->
                val repository = SqliteGenerationRepository(db, options, clock)
                val job = assertIs<Submission.Accepted>(repository.start(ownerId, requestKey(), "request", command())).job
                val cancelling =
                    GenerationWorker(
                        repository,
                        TextGenerator { throw CancellationException("process stopped") },
                        FakeCoverGenerator(options),
                        options,
                    )
                assertFailsWith<CancellationException> { cancelling.run(assertNotNull(repository.claim())) }
                assertEquals(Stage.CREATING_TEXT, repository.status(ownerId, job.id)?.stage)
                db.write { StoryJobs.update({ StoryJobs.id eq job.id.value }) { it[leaseUntil] = 0 } }
                val failingStorage =
                    object : GenerationRepository by repository {
                        override suspend fun publish(
                            execution: Execution,
                            cover: ByteArray,
                        ): StoryId? = error("Storage temporarily unavailable")
                    }
                val text = TextGenerator { GeneratedText("Title", "Text") }
                assertFailsWith<IllegalStateException> {
                    GenerationWorker(failingStorage, text, FakeCoverGenerator(options), options).run(assertNotNull(repository.claim()))
                }
                val status = assertNotNull(repository.status(ownerId, job.id))
                assertEquals(Stage.CREATING_COVER, status.stage)
                assertNull(status.failure)
                db.write { StoryJobs.update({ StoryJobs.id eq job.id.value }) { it[leaseUntil] = 0 } }
                val recovered =
                    GenerationWorker(
                        repository,
                        TextGenerator { error("Persisted text must be reused") },
                        FakeCoverGenerator(options),
                        options,
                    )
                recovered.run(assertNotNull(repository.claim()))
                assertEquals(Stage.READY, repository.status(ownerId, job.id)?.stage)
            }
        }

    @Test
    fun `scheduler polls a single queue when idle`() =
        runBlocking {
            val claims = Channel<Unit>(Channel.UNLIMITED)
            val repository =
                object : GenerationRepository by UnusedGenerationRepository() {
                    override suspend fun claim(): Execution? {
                        claims.send(Unit)
                        return null
                    }
                }
            val worker = GenerationWorker(repository, TextGenerator { error("No queued stories") }, FakeCoverGenerator(options), options)
            val scheduler = StoryGenerationScheduler(repository, worker, options).start(this)
            try {
                withTimeout(1_000) { claims.receive() }
                assertNull(withTimeoutOrNull(1_000) { claims.receive() })
                withTimeout(6_000) { claims.receive() }
            } finally {
                scheduler.cancelAndJoin()
            }
        }

    @Test fun `configured concurrency bounds active generator calls across owners`() =
        runBlocking {
            SqliteDatabase(dbConfig).use { db ->
                val repository = SqliteGenerationRepository(db, options, clock)
                val jobs =
                    (1..5).map { index ->
                        val owner = requireNotNull(OwnerId.parse("owner-019a0000-0000-7000-8000-00000000000$index"))
                        owner to assertIs<Submission.Accepted>(repository.start(owner, requestKey(), "request", command())).job
                    }
                var active = 0
                var maximum = 0
                var startedCount = 0
                val started = Channel<Unit>(Channel.UNLIMITED)
                val finishFirst = CompletableDeferred<Unit>()
                val finishOthers = CompletableDeferred<Unit>()
                val text =
                    TextGenerator {
                        val finish = if (++startedCount == 1) finishFirst else finishOthers
                        active++
                        maximum = maxOf(maximum, active)
                        try {
                            started.send(Unit)
                            finish.await()
                            GeneratedText("Title", "Text")
                        } finally {
                            active--
                        }
                    }
                val worker = GenerationWorker(repository, text, FakeCoverGenerator(options), options)
                val scheduler = StoryGenerationScheduler(repository, worker, options).start(this)
                try {
                    withTimeout(5_000) { repeat(2) { started.receive() } }
                    assertEquals(2, active)
                    finishFirst.complete(Unit)
                    withTimeout(2_000) { started.receive() }
                    assertEquals(2, active)
                    finishOthers.complete(Unit)
                    withTimeout(5_000) {
                        while (jobs.any { (owner, job) ->
                                repository.status(owner, job.id)?.stage != Stage.READY
                            }
                        ) {
                            delay(10)
                        }
                    }
                    assertEquals(2, maximum)
                } finally {
                    scheduler.cancelAndJoin()
                }
            }
        }
}
