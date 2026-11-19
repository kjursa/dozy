package com.dozy.features.characteravatar

import com.dozy.config.AvatarConfig
import com.dozy.config.DatabaseConfig
import com.dozy.config.FakeAvatarMode
import com.dozy.features.characteravatar.application.AvatarWorker
import com.dozy.features.characteravatar.data.FakeAvatarGenerator
import com.dozy.features.characteravatar.data.LocalAvatarStorage
import com.dozy.features.characteravatar.data.repository.SqliteAvatarJobs
import com.dozy.features.characteravatar.domain.model.AvatarInput
import com.dozy.features.characteravatar.domain.model.AvatarState
import com.dozy.features.characteravatar.domain.repository.AvatarGenerator
import com.dozy.features.character.data.repository.SqliteCharacterRepository
import com.dozy.features.character.domain.model.Character
import com.dozy.features.character.domain.model.SaveCharacter
import com.dozy.features.character.domain.model.SaveCharacterResult
import com.dozy.features.installation.domain.model.InstallationId
import com.dozy.infrastructure.database.SqliteDatabase
import java.io.ByteArrayInputStream
import java.nio.file.Path
import java.util.UUID
import javax.imageio.ImageIO
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith
import org.junit.jupiter.api.io.TempDir

class AvatarJobsTest {
    @TempDir lateinit var directory: Path
    private val owner = InstallationId("019a0000-0000-7000-8000-000000000001")
    private val other = InstallationId("019a0000-0000-7000-8000-000000000002")
    private val input get() = (AvatarInput.parse("CHILD", " Kind ", " Green eyes ") as AvatarInput.Result.Valid).input
    private val databaseConfig get() = DatabaseConfig(directory.resolve("dozy.db").toString(), 5000)
    private val config get() = AvatarConfig(storagePath = directory.resolve("assets").toString(), delayMillis = 0)

    @Test fun recoveryPersistenceAndOwnedAssetCanSaveCharacter() = runBlocking {
        val job = SqliteDatabase(databaseConfig).use { database ->
            val jobs = SqliteAvatarJobs(database, config)
            val created = jobs.create(owner, input)
            assertEquals(7, UUID.fromString(created.id).version())
            assertEquals(7, UUID.fromString(created.assetId).version())
            assertEquals(AvatarState.QUEUED, created.state)
            assertEquals(AvatarState.RUNNING, jobs.claim()?.state)
            assertNull(jobs.find(other, created.id))
            assertNull(jobs.find(owner, "unknown"))
            created
        }
        SqliteDatabase(databaseConfig).use { database ->
            val jobs = SqliteAvatarJobs(database, config)
            val storage = LocalAvatarStorage(config)
            jobs.recover()
            val worker = AvatarWorker(jobs, FakeAvatarGenerator(config), storage, config)
            assertTrue(worker.processNext())
            assertFalse(worker.processNext())
            assertEquals(AvatarState.READY, jobs.find(owner, job.id)?.state)
            jobs.complete(job, true)
            val bytes = assertNotNull(storage.get(job.assetId))
            assertEquals(64, ImageIO.read(ByteArrayInputStream(bytes)).width)
            assertTrue(bytes.contentEquals(assertNotNull(LocalAvatarStorage(config).get(job.assetId))))
            val avatar = Character.Avatar(job.assetId, job.url)
            val command = (SaveCharacter.parse("Hero", input.personality, input.appearance, input.type, avatar) as SaveCharacter.Result.Valid).command
            val characters = SqliteCharacterRepository(database)
            assertTrue(characters.create(owner, command) is SaveCharacterResult.Created)
            assertEquals(
                SaveCharacterResult.ReferenceUnavailable,
                characters.create(other, command)
            )
        }
        SqliteDatabase(databaseConfig).use { database ->
            assertEquals(AvatarState.READY, SqliteAvatarJobs(database, config).find(owner, job.id)?.state)
        }
    }

    @Test fun failureAndCancelledWorkRemainRecoverable() = runBlocking {
        SqliteDatabase(databaseConfig).use { database ->
            val failureConfig = config.copy(mode = FakeAvatarMode.FAILURE)
            val jobs = SqliteAvatarJobs(database, failureConfig)
            val storage = LocalAvatarStorage(failureConfig)
            val failed = jobs.create(owner, input)
            AvatarWorker(jobs, FakeAvatarGenerator(failureConfig), storage, failureConfig).processNext()
            assertEquals(AvatarState.FAILED, jobs.find(owner, failed.id)?.state)
            assertNull(storage.get(failed.assetId))
            val interrupted = jobs.create(owner, input)
            assertFailsWith<CancellationException> {
                AvatarWorker(jobs, AvatarGenerator { throw CancellationException() }, storage, config).processNext()
            }
            assertEquals(AvatarState.RUNNING, jobs.find(owner, interrupted.id)?.state)
            jobs.recover()
            assertEquals(AvatarState.QUEUED, jobs.find(owner, interrupted.id)?.state)
            assertTrue(failed.id != interrupted.id)
        }
    }

    @Test fun workersBoundConcurrencyAndNeverHoldTransactionDuringGeneration() = runBlocking {
        SqliteDatabase(databaseConfig).use { database ->
            val workerConfig = config.copy(concurrency = 2)
            val jobs = SqliteAvatarJobs(database, workerConfig)
            repeat(5) { jobs.create(owner, input) }
            val started = AtomicInteger()
            val active = AtomicInteger()
            val maximum = AtomicInteger()
            val bothStarted = CompletableDeferred<Unit>()
            val generator = AvatarGenerator { input ->
                val count = active.incrementAndGet()
                maximum.accumulateAndGet(count, ::maxOf)
                if (started.incrementAndGet() == 2) bothStarted.complete(Unit)
                try { awaitCancellation() } finally { active.decrementAndGet() }
            }
            val workers = AvatarWorker(jobs, generator, LocalAvatarStorage(workerConfig), workerConfig).start(this)
            try {
                withTimeout(5000) { bothStarted.await() }
                // A write must finish while both generators are suspended.
                withTimeout(5000) { jobs.create(owner, input) }
                assertEquals(2, maximum.get())
                assertEquals(2, started.get())
            } finally { workers.cancelAndJoin() }
            assertEquals(0, active.get())
        }
    }

    @Test fun validatesDescriptionsAndDevelopmentConfig() {
        assertEquals(AvatarInput.Result.Invalid(SaveCharacter.Field.PERSONALITY, SaveCharacter.Reason.REQUIRED), AvatarInput.parse("CHILD", " ", "eyes"))
        assertEquals(AvatarInput.Result.Invalid(SaveCharacter.Field.TYPE, SaveCharacter.Reason.INVALID_VALUE), AvatarInput.parse("unknown", "kind", "eyes"))
        assertFailsWith<IllegalArgumentException> { config.copy(publicBaseUrl = "http://localhost:8080") }
        assertFailsWith<IllegalArgumentException> { config.copy(publicBaseUrl = "http://example.com", allowLocalHttp = true) }
        assertFailsWith<IllegalArgumentException> { config.copy(concurrency = 0) }
        config.copy(publicBaseUrl = "http://10.0.2.2:8080", allowLocalHttp = true)
    }
}
