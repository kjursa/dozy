package com.dozy.features.story

import com.dozy.config.DatabaseConfig
import com.dozy.config.GenerationConfig
import com.dozy.domain.id.AssetId
import com.dozy.domain.id.JobId
import com.dozy.domain.id.OwnerId
import com.dozy.domain.id.StoryId
import com.dozy.features.asset.data.database.Assets
import com.dozy.features.asset.data.repository.SqliteAssetRepository
import com.dozy.features.story.application.GenerationWorker
import com.dozy.features.story.data.FakeCoverGenerator
import com.dozy.features.story.data.FakeTextGenerator
import com.dozy.features.story.data.repository.SqliteGenerationRepository
import com.dozy.features.story.domain.model.AvatarSnapshot
import com.dozy.features.story.domain.model.Failure
import com.dozy.features.story.domain.model.GeneratedText
import com.dozy.features.story.domain.model.RetrySubmission
import com.dozy.features.story.domain.model.Stage
import com.dozy.features.story.domain.model.StoryConfiguration
import com.dozy.features.story.domain.model.Submission
import com.dozy.infrastructure.database.SqliteDatabase
import com.dozy.infrastructure.database.registerOwnerPartition
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.update
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GenerationRepositoryTest {
    private val clock = Clock.systemUTC()
    @TempDir lateinit var directory: Path
    private val ownerId = requireNotNull(OwnerId.parse("owner-019a0000-0000-7000-8000-000000000010"))
    private val stranger = requireNotNull(OwnerId.parse("owner-019a0000-0000-7000-8000-000000000011"))
    private val dbConfig get() = DatabaseConfig(directory.resolve("dozy.db").toString(), 5000)
    private val config = GenerationConfig(delayMillis = 0, leaseMillis = 300)

    @Test fun `fail once retry survives restart replays before lifecycle and publishes once`() = runBlocking {
        val options = config.copy(textMode = GenerationConfig.TextMode.FAIL_ONCE)
        val first = SqliteDatabase(dbConfig)
        val repository = SqliteGenerationRepository(first, options, clock)
        val originalKey = requestKey()
        val job = assertIs<Submission.Accepted>(repository.start(ownerId, originalKey, "snapshot", command())).job
        val worker = GenerationWorker(repository, FakeTextGenerator(options), FakeCoverGenerator(options), options)
        worker.run(assertNotNull(repository.claim()))
        assertEquals(Stage.FAILED, repository.status(ownerId, job.id)?.stage)
        assertIs<RetrySubmission.IdempotencyConflict>(repository.retry(ownerId, job.id, originalKey))
        assertIs<RetrySubmission.JobNotFound>(repository.retry(stranger, job.id, requestKey()))
        val key = requestKey()
        val accepted = assertIs<RetrySubmission.Accepted>(repository.retry(ownerId, job.id, key))
        assertEquals(job, accepted.job)
        assertIs<RetrySubmission.IdempotencyConflict>(repository.retry(ownerId, JobId.generate(), key))
        assertIs<Submission.IdempotencyConflict>(repository.start(ownerId, key, "snapshot", command()))
        assertIs<RetrySubmission.JobNotFailed>(repository.retry(ownerId, job.id, requestKey()))
        first.close()
        SqliteDatabase(dbConfig).use { second ->
            val recovered = SqliteGenerationRepository(second, options, clock)
            assertEquals(accepted.copy(isReplay = true), recovered.retry(ownerId, job.id, key))
            GenerationWorker(recovered, FakeTextGenerator(options), FakeCoverGenerator(options), options).run(assertNotNull(recovered.claim()))
            val ready = assertNotNull(recovered.status(ownerId, job.id))
            assertEquals(Stage.READY, ready.stage)
            assertEquals(Stage.READY, assertIs<RetrySubmission.Accepted>(recovered.retry(ownerId, job.id, key)).job.stage)
            assertEquals(command().configuration, recovered.read(ownerId, requireNotNull(ready.storyId))?.configuration)
            assertNull(recovered.claim())
        }
    }

    @Test fun `concurrent retries archive each attempt and stale results cannot change newer execution`() = runBlocking {
        SqliteDatabase(dbConfig).use { db ->
            val repository = SqliteGenerationRepository(db, config, clock)
            val job = assertIs<Submission.Accepted>(repository.start(ownerId, requestKey(), "snapshot", command())).job
            val stale = assertNotNull(repository.claim())
            repository.fail(stale, Failure.GENERATION_FAILED)
            val key = requestKey()
            val results = List(8) { async { repository.retry(ownerId, job.id, key) } }.awaitAll()
            results.forEach { assertEquals(job, assertIs<RetrySubmission.Accepted>(it).job) }
            val execution = assertNotNull(repository.claim())
            assertEquals(2, execution.ordinal)
            assertFalse(repository.saveText(stale, GeneratedText("Stale", "Stale")))
            assertNull(repository.publish(stale, byteArrayOf(1)))
            repository.fail(stale, Failure.CONTENT_REJECTED)
            assertEquals(Stage.CREATING_TEXT, repository.status(ownerId, job.id)?.stage)
            repository.fail(execution, Failure.GENERATION_FAILED)
            val competing = assertIs<Submission.Accepted>(repository.start(ownerId, requestKey(), "snapshot", command())).job
            assertIs<RetrySubmission.ActiveJobExists>(repository.retry(ownerId, job.id, requestKey()))
            // Recorded binding wins over the different active job and FAILED lifecycle.
            assertEquals(Stage.FAILED, assertIs<RetrySubmission.Accepted>(repository.retry(ownerId, job.id, key)).job.stage)
            repository.fail(assertNotNull(repository.claim()), Failure.GENERATION_FAILED)
            val newKeys = List(8) { requestKey() }
            val attempts = newKeys.map { async { repository.retry(ownerId, job.id, it) } }.awaitAll()
            assertEquals(1, attempts.count { it is RetrySubmission.Accepted })
            assertEquals(7, attempts.count { it is RetrySubmission.JobNotFailed })
            assertEquals(Stage.FAILED, assertIs<RetrySubmission.Accepted>(repository.retry(ownerId, job.id, key)).job.stage)
            assertEquals(Stage.FAILED, repository.status(ownerId, competing.id)?.stage)
            val latest = assertNotNull(repository.claim())
            assertEquals(3, latest.ordinal)
            assertTrue(repository.saveText(latest, GeneratedText("Final", "Final")))
            assertNotNull(repository.publish(latest, byteArrayOf(1)))
            assertNull(repository.publish(latest, byteArrayOf(1)))
            repository.fail(execution, Failure.GENERATION_FAILED)
            assertEquals(Stage.READY, repository.status(ownerId, job.id)?.stage)
            assertEquals(Stage.FAILED, assertIs<RetrySubmission.Accepted>(repository.retry(ownerId, job.id, key)).job.stage)
        }
    }

    @Test fun `atomic concurrent start replay ordering active limit and isolation`() = runBlocking {
        SqliteDatabase(dbConfig).use { db ->
            val repository = SqliteGenerationRepository(db, config, clock)
            val key = requestKey()
            val results = List(8) { async { repository.start(ownerId, key, "original", command()) } }.awaitAll()
            val job = assertIs<Submission.Accepted>(results.first()).job
            assertEquals(7, UUID.fromString(job.id.value.removePrefix("job-")).version())
            results.forEach { assertEquals(job, assertIs<Submission.Accepted>(it).job) }
            assertIs<Submission.IdempotencyConflict>(repository.start(ownerId, key, "different", command()))
            assertIs<Submission.ActiveJobExists>(repository.start(ownerId, requestKey(), "original", command()))
            assertNull(repository.status(stranger, job.id))
            assertNull(repository.read(ownerId, StoryId.generate()))
            assertIs<Submission.Accepted>(repository.start(stranger, key, "original", command()))
        }
    }
    @Test fun `avatar ownership ready status and URL must match but replay retains accepted snapshots`() = runBlocking {
        SqliteDatabase(dbConfig).use { db ->
            val repository = SqliteGenerationRepository(db, config, clock)
            val assetId = AssetId.generate()
            val url = "https://example.test/api/v1/assets/${assetId.value}"
            db.write {
                registerOwnerPartition(ownerId)
                Assets.insert { it[id] = assetId.value; it[Assets.ownerId] = this@GenerationRepositoryTest.ownerId.value; it[Assets.url] = url; it[status] = "READY" }
            }
            val input = configuration().copy(cast = configuration().cast.map { it.copy(avatar = AvatarSnapshot(assetId, url)) })
            assertIs<Submission.ReferenceUnavailable>(repository.start(stranger, requestKey(), "foreign", command(input)))
            assertIs<Submission.ReferenceUnavailable>(repository.start(ownerId, requestKey(), "wrong URL", command(input.copy(cast = input.cast.map { it.copy(avatar = AvatarSnapshot(assetId, "$url/changed")) }))))
            val key = requestKey()
            val accepted = repository.start(ownerId, key, "original", command(input))
            assertIs<Submission.Accepted>(accepted)
            db.write { Assets.update({ Assets.id eq assetId.value }) { it[status] = "FAILED" } }
            assertEquals(accepted, repository.start(ownerId, key, "original", command(input)))
        }
    }
    @Test fun `different simultaneous keys permit exactly one active job`() = runBlocking {
        SqliteDatabase(dbConfig).use { db ->
            val repository = SqliteGenerationRepository(db, config, clock)
            val results = List(8) { async { repository.start(ownerId, requestKey(), "request", command()) } }.awaitAll()
            assertEquals(1, results.count { it is Submission.Accepted })
            assertEquals(7, results.count { it is Submission.ActiveJobExists })
        }
    }
    @Test fun `recover cover stage after restart fence stale worker and publish exactly once`() = runBlocking {
        val key = requestKey()
        val first = SqliteDatabase(dbConfig)
        val startedAt = Instant.parse("2026-11-19T12:00:00Z")
        val startedClock = Clock.fixed(startedAt, ZoneOffset.UTC)
        val repository = SqliteGenerationRepository(first, config, startedClock)
        val job = assertIs<Submission.Accepted>(repository.start(ownerId, key, "request", command())).job
        val abandoned = assertNotNull(repository.claim())
        assertEquals(Stage.CREATING_TEXT, abandoned.job.stage)
        assertNull(repository.claim())
        assertNull(repository.status(ownerId, job.id)?.storyId)
        assertTrue(repository.saveText(abandoned, GeneratedText("Immutable title", "Immutable text")))
        assertEquals(Stage.CREATING_COVER, repository.status(ownerId, job.id)?.stage)
        first.close()
        SqliteDatabase(dbConfig).use { second ->
            val justBeforeExpiry = Clock.offset(startedClock, Duration.ofMillis(config.leaseMillis - 1))
            assertNull(SqliteGenerationRepository(second, config, justBeforeExpiry).claim())
            val atExpiry = Clock.offset(startedClock, Duration.ofMillis(config.leaseMillis))
            val recovered = SqliteGenerationRepository(second, config, atExpiry)
            val execution = assertNotNull(recovered.claim())
            assertEquals(Stage.CREATING_COVER, execution.job.stage)
            assertEquals("Immutable text", execution.text)
            assertFalse(recovered.saveText(abandoned, GeneratedText("Changed", "Changed")))
            val bytes = FakeCoverGenerator(config).generate(configuration())
            assertNotNull(ImageIO.read(bytes.inputStream()))
            assertNull(recovered.publish(abandoned, bytes))
            assertNotNull(recovered.publish(execution, bytes))
            assertNull(recovered.publish(execution, bytes))
            val ready = assertNotNull(recovered.status(ownerId, job.id))
            assertEquals(Stage.READY, ready.stage)
            assertNull(ready.failure)
            val story = assertNotNull(recovered.read(ownerId, assertNotNull(ready.storyId)))
            assertEquals(7, UUID.fromString(story.id.value.removePrefix("story-")).version())
            assertEquals("Immutable title", story.title)
            assertEquals("Immutable text", story.text)
            assertEquals("Find a star", story.configuration.idea)
            assertEquals(atExpiry.millis(), story.createdAt)
            assertEquals(story, recovered.read(ownerId, story.id))
            assertNull(recovered.read(stranger, story.id))
            val assetId = requireNotNull(AssetId.parse(story.coverUrl.substringAfterLast('/')))
            assertContentEquals(bytes, SqliteAssetRepository(second).read(ownerId, assetId))
            assertNull(SqliteAssetRepository(second).read(stranger, assetId))
            assertNull(SqliteAssetRepository(second).read(ownerId, AssetId.generate()))
            assertEquals(ready, assertIs<Submission.Accepted>(recovered.start(ownerId, key, "request", command())).job)
            assertIs<Submission.IdempotencyConflict>(recovered.start(ownerId, key, "changed", command()))
            recovered.fail(abandoned, Failure.GENERATION_FAILED)
            assertEquals(ready, recovered.status(ownerId, job.id))
        }
    }
    @Test fun `worker terminal modes text lengths and protected reference availability`() = runBlocking {
        SqliteDatabase(dbConfig).use { db ->
            for ((textMode, coverMode, expected) in listOf(
                Triple(GenerationConfig.TextMode.SUCCESS, GenerationConfig.CoverMode.SUCCESS, null),
                Triple(GenerationConfig.TextMode.FAILURE, GenerationConfig.CoverMode.SUCCESS, Failure.GENERATION_FAILED),
                Triple(GenerationConfig.TextMode.CONTENT_REJECTED, GenerationConfig.CoverMode.SUCCESS, Failure.CONTENT_REJECTED),
                Triple(GenerationConfig.TextMode.SUCCESS, GenerationConfig.CoverMode.FAILURE, Failure.GENERATION_FAILED),
            )) {
                val options = config.copy(textMode = textMode, coverMode = coverMode)
                val repository = SqliteGenerationRepository(db, options, clock)
                val input = command()
                val avatarInput = command(input.configuration.copy(cast = input.configuration.cast.map { it.copy(avatar = AvatarSnapshot(AssetId.generate(), "https://example.test/missing")) }))
                assertIs<Submission.ReferenceUnavailable>(repository.start(ownerId, requestKey(), "unavailable", avatarInput))
                val job = assertIs<Submission.Accepted>(repository.start(ownerId, requestKey(), "request", input)).job
                GenerationWorker(repository, FakeTextGenerator(options), FakeCoverGenerator(options), options).run(assertNotNull(repository.claim()))
                val status = assertNotNull(repository.status(ownerId, job.id))
                assertEquals(expected, status.failure)
                assertEquals(if (expected == null) Stage.READY else Stage.FAILED, status.stage)
                assertEquals(expected == null, status.storyId != null)
                if (status.storyId != null) {
                    val story = assertNotNull(repository.read(ownerId, status.storyId!!))
                    assertTrue(story.text.contains("Luna") && story.text.contains("Moon garden"))
                    assertTrue(story.text.split(Regex("\\s+")).size in 550..650)
                }
                assertNull(repository.claim())
            }
            val long = FakeTextGenerator(config).generate(configuration().copy(length = StoryConfiguration.Length.LONG))
            assertTrue(long.text.split(Regex("\\s+")).size in 1400..1600)
        }
    }
}
