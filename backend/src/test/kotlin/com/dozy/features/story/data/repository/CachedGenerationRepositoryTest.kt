package com.dozy.features.story.data.repository

import com.dozy.MutableClock
import com.dozy.config.DatabaseConfig
import com.dozy.config.GenerationConfig
import com.dozy.domain.id.IdempotencyKey
import com.dozy.domain.id.JobId
import com.dozy.domain.id.OwnerId
import com.dozy.features.story.UnusedGenerationRepository
import com.dozy.features.story.command
import com.dozy.features.story.data.cache.KeyValueJobStatusCache
import com.dozy.features.story.domain.model.Failure
import com.dozy.features.story.domain.model.GeneratedText
import com.dozy.features.story.domain.model.Job
import com.dozy.features.story.domain.model.RetrySubmission
import com.dozy.features.story.domain.model.Stage
import com.dozy.features.story.domain.model.Submission
import com.dozy.features.story.domain.repository.GenerationRepository
import com.dozy.features.story.requestKey
import com.dozy.infrastructure.database.SqliteDatabase
import com.dozy.infrastructure.keyvalue.InMemoryKeyValueStore
import com.dozy.infrastructure.keyvalue.KeyValueStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.io.TempDir
import java.io.IOException
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class CachedGenerationRepositoryTest {
    @TempDir
    lateinit var directory: Path

    private val ownerId = OwnerId.generate()
    private val clock = MutableClock()

    @Test
    fun pollsFromTheCacheUntilExpiryAndKeepsOwnersIsolated() =
        runBlocking {
            database().use { database ->
                val sqlite = SqliteGenerationRepository(database, GenerationConfig(), clock)
                var statusReads = 0
                val source =
                    object : GenerationRepository by sqlite {
                        override suspend fun status(
                            ownerId: OwnerId,
                            id: JobId,
                        ): Job? {
                            statusReads++
                            return sqlite.status(ownerId, id)
                        }
                    }
                val repository = CachedGenerationRepository(source, cache())
                val job = assertIs<Submission.Accepted>(repository.start(ownerId, requestKey(), "request", command())).job

                repeat(2) { assertEquals(job, repository.status(ownerId, job.id)) }
                assertEquals(1, statusReads)
                assertNull(repository.status(OwnerId.generate(), job.id))
                assertEquals(2, statusReads)

                clock.advance(5.seconds)
                assertEquals(job, repository.status(ownerId, job.id))
                assertEquals(3, statusReads)
            }
        }

    @Test
    fun transitionsAndRetryInvalidateStatusImmediately() =
        runBlocking {
            database().use { database ->
                val repository = CachedGenerationRepository(SqliteGenerationRepository(database, GenerationConfig(), clock), cache())
                val job = assertIs<Submission.Accepted>(repository.start(ownerId, requestKey(), "request", command())).job
                assertEquals(Stage.QUEUED, repository.status(ownerId, job.id)?.stage)

                val first = assertNotNull(repository.claim())
                assertEquals(Stage.CREATING_TEXT, repository.status(ownerId, job.id)?.stage)
                repository.fail(first, Failure.CONTENT_REJECTED)
                assertEquals(Job(job.id, Stage.FAILED, failure = Failure.CONTENT_REJECTED), repository.status(ownerId, job.id))

                assertIs<RetrySubmission.Accepted>(repository.retry(ownerId, job.id, requestKey()))
                assertEquals(job, repository.status(ownerId, job.id))
                val retry = assertNotNull(repository.claim())
                assertEquals(Stage.CREATING_TEXT, repository.status(ownerId, job.id)?.stage)

                assertTrue(repository.saveText(retry, GeneratedText("Title", "Text")))
                assertEquals(Stage.CREATING_COVER, repository.status(ownerId, job.id)?.stage)
                assertTrue(repository.publish(retry, byteArrayOf(1)))
                repeat(2) {
                    val ready = assertNotNull(repository.status(ownerId, job.id))
                    assertEquals(Stage.READY, ready.stage)
                    assertNotNull(ready.storyId)
                }
            }
        }

    @Test
    fun aConcurrentCacheMissCannotPutBackAnOldStatusAfterInvalidation() =
        runBlocking {
            val failed = Job(JobId.generate(), Stage.FAILED, failure = Failure.GENERATION_FAILED)
            val queued = Job(failed.id, Stage.QUEUED)
            var status = failed
            val readStarted = CompletableDeferred<Unit>()
            val finishRead = CompletableDeferred<Unit>()
            val retryCommitted = CompletableDeferred<Unit>()
            val source =
                object : GenerationRepository by UnusedGenerationRepository() {
                    override suspend fun status(
                        ownerId: OwnerId,
                        id: JobId,
                    ): Job {
                        val snapshot = status
                        if (snapshot == failed) {
                            readStarted.complete(Unit)
                            finishRead.await()
                        }
                        return snapshot
                    }

                    override suspend fun retry(
                        ownerId: OwnerId,
                        id: JobId,
                        key: IdempotencyKey,
                    ): RetrySubmission {
                        status = queued
                        retryCommitted.complete(Unit)
                        return RetrySubmission.Accepted(queued)
                    }
                }
            val repository = CachedGenerationRepository(source, cache())

            val reading = async { repository.status(ownerId, failed.id) }
            withTimeout(5.seconds) {
                try {
                    readStarted.await()
                    val retrying = async { repository.retry(ownerId, failed.id, requestKey()) }
                    retryCommitted.await()
                    finishRead.complete(Unit)
                    assertEquals(failed, reading.await())
                    assertEquals(RetrySubmission.Accepted(queued), retrying.await())
                } finally {
                    finishRead.complete(Unit)
                }
            }
            assertEquals(queued, repository.status(ownerId, failed.id))
        }

    @Test
    fun unavailableCacheFallsBackToTheRepositoryAndDoesNotFailWrites() =
        runBlocking {
            database().use { database ->
                val unavailable =
                    object : KeyValueStore {
                        override suspend fun get(key: String): String? = throw IOException("Unavailable")

                        override suspend fun put(
                            key: String,
                            value: String,
                            ttl: Duration,
                        ) = throw IOException("Unavailable")

                        override suspend fun delete(key: String) = throw IOException("Unavailable")
                    }
                val repository =
                    CachedGenerationRepository(
                        SqliteGenerationRepository(database, GenerationConfig(), clock),
                        KeyValueJobStatusCache(unavailable),
                    )
                val job = assertIs<Submission.Accepted>(repository.start(ownerId, requestKey(), "request", command())).job
                assertEquals(job, repository.status(ownerId, job.id))
                assertNotNull(repository.claim())
                assertEquals(Stage.CREATING_TEXT, repository.status(ownerId, job.id)?.stage)
            }
        }

    @Test
    fun preservesCancellationFromCacheReads() =
        runBlocking {
            val cancelled =
                object : KeyValueStore by InMemoryKeyValueStore(clock) {
                    override suspend fun get(key: String): String? = throw CancellationException()
                }
            val repository = CachedGenerationRepository(UnusedGenerationRepository(), KeyValueJobStatusCache(cancelled))
            assertFailsWith<CancellationException> { repository.status(ownerId, JobId.generate()) }
            Unit
        }

    private fun database() = SqliteDatabase(DatabaseConfig(directory.resolve("jobs.db").toString(), 5000))

    private fun cache() = KeyValueJobStatusCache(InMemoryKeyValueStore(clock))
}
