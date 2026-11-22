package com.dozy.features.story.data.repository

import com.dozy.features.story.testGenerationRepository
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
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.io.TempDir
import java.io.IOException
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
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
                val source = CountingRepository(testGenerationRepository(database, GenerationConfig(), clock))
                val repository = CachedGenerationRepository(source, cache())
                val job = assertIs<Submission.Accepted>(repository.start(ownerId, requestKey(), "request", command())).job

                assertEquals(job, repository.status(ownerId, job.id))
                repeat(29) {
                    clock.advance(10.seconds)
                    assertEquals(job, repository.status(ownerId, job.id))
                }
                assertEquals(0, source.statusReads)
                assertNull(repository.status(OwnerId.generate(), job.id))
                assertEquals(1, source.statusReads)

                clock.advance(10.seconds)
                assertEquals(job, repository.status(ownerId, job.id))
                assertEquals(2, source.statusReads)
            }
        }

    @Test
    fun transitionsAndRetriesKeepStatusAvailableWithoutPollingSqlite() =
        runBlocking {
            database().use { database ->
                val source = CountingRepository(testGenerationRepository(database, GenerationConfig(), clock))
                val repository = CachedGenerationRepository(source, cache())
                val job = assertIs<Submission.Accepted>(repository.start(ownerId, requestKey(), "request", command())).job
                assertEquals(Stage.QUEUED, repository.status(ownerId, job.id)?.stage)

                val first = assertNotNull(repository.claim())
                assertEquals(Stage.CREATING_TEXT, repository.status(ownerId, job.id)?.stage)
                assertTrue(repository.fail(first, Failure.CONTENT_REJECTED))
                assertEquals(Job(job.id, Stage.FAILED, failure = Failure.CONTENT_REJECTED), repository.status(ownerId, job.id))

                val firstRetryKey = requestKey()
                assertIs<RetrySubmission.Accepted>(repository.retry(ownerId, job.id, firstRetryKey))
                assertEquals(job, repository.status(ownerId, job.id))
                val retry = assertNotNull(repository.claim())
                assertFalse(repository.fail(first, Failure.CONTENT_REJECTED))
                assertEquals(Stage.CREATING_TEXT, repository.status(ownerId, job.id)?.stage)
                assertTrue(repository.fail(retry, Failure.GENERATION_FAILED))

                assertIs<RetrySubmission.Accepted>(repository.retry(ownerId, job.id, requestKey()))
                val current = assertNotNull(repository.claim())
                val replay = assertIs<RetrySubmission.Accepted>(repository.retry(ownerId, job.id, firstRetryKey))
                assertTrue(replay.isReplay)
                assertEquals(Stage.FAILED, replay.job.stage)
                assertEquals(Stage.CREATING_TEXT, repository.status(ownerId, job.id)?.stage)

                assertTrue(repository.saveText(current, GeneratedText("Title", "Text")))
                assertEquals(Stage.CREATING_COVER, repository.status(ownerId, job.id)?.stage)
                clock.advance(10.seconds)
                val storyId = assertNotNull(repository.publish(current, byteArrayOf(1)))
                val ready = Job(job.id, Stage.READY, storyId = storyId)
                assertEquals(ready, repository.status(ownerId, job.id))

                // The previous stage's TTL has elapsed and READY is still cached.
                clock.advance(295.seconds)
                assertEquals(ready, repository.status(ownerId, job.id))
                assertEquals(0, source.statusReads)
            }
        }

    @Test
    fun aConcurrentCacheMissCannotOverwriteACommittedTransition() =
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
                    val retrying = async(start = CoroutineStart.UNDISPATCHED) { repository.retry(ownerId, failed.id, requestKey()) }
                    assertFalse(retryCommitted.isCompleted)
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
                        testGenerationRepository(database, GenerationConfig(), clock),
                        KeyValueJobStatusCache(unavailable),
                    )
                val job = assertIs<Submission.Accepted>(repository.start(ownerId, requestKey(), "request", command())).job
                assertEquals(job, repository.status(ownerId, job.id))
                assertNotNull(repository.claim())
                assertEquals(Stage.CREATING_TEXT, repository.status(ownerId, job.id)?.stage)
            }
        }

    @Test
    fun failedCacheUpdateRemovesThePreviouslyCachedStatus() =
        runBlocking {
            database().use { database ->
                val memory = InMemoryKeyValueStore(clock)
                var failWrites = false
                val store =
                    object : KeyValueStore by memory {
                        override suspend fun put(
                            key: String,
                            value: String,
                            ttl: Duration,
                        ) {
                            if (failWrites) throw IOException("Unavailable")
                            memory.put(key, value, ttl)
                        }
                    }
                val source = CountingRepository(testGenerationRepository(database, GenerationConfig(), clock))
                val repository = CachedGenerationRepository(source, KeyValueJobStatusCache(store))
                val job = assertIs<Submission.Accepted>(repository.start(ownerId, requestKey(), "request", command())).job
                failWrites = true
                assertNotNull(repository.claim())
                assertEquals(Stage.CREATING_TEXT, repository.status(ownerId, job.id)?.stage)
                assertEquals(1, source.statusReads)
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

    private class CountingRepository(
        private val repository: GenerationRepository,
    ) : GenerationRepository by repository {
        var statusReads = 0
            private set

        override suspend fun status(
            ownerId: OwnerId,
            id: JobId,
        ): Job? {
            statusReads++
            return repository.status(ownerId, id)
        }
    }

    private fun database() = SqliteDatabase(DatabaseConfig(directory.resolve("jobs.db").toString(), 5000))

    private fun cache() = KeyValueJobStatusCache(InMemoryKeyValueStore(clock))
}
