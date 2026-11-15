package com.dozy.features.character.data.repository

import com.dozy.config.DatabaseConfig
import com.dozy.features.asset.data.database.Assets
import com.dozy.features.character.data.database.Characters
import com.dozy.features.character.domain.model.Character
import com.dozy.features.character.domain.model.SaveCharacter
import com.dozy.features.character.domain.model.SaveCharacterResult
import com.dozy.features.installation.data.database.registerInstallation
import com.dozy.features.installation.domain.model.InstallationId
import com.dozy.infrastructure.database.SqliteDatabase
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.update
import org.junit.jupiter.api.io.TempDir

class SqliteCharacterRepositoryTest {
    @TempDir lateinit var directory: Path
    private val owner = InstallationId("owner")
    private val stranger = InstallationId("stranger")
    private val id = Character.Id("019a0000-0000-7000-8000-000000000001")
    private fun database() = SqliteDatabase(DatabaseConfig(directory.resolve("dozy.db").toString(), 5000))
    private fun command(name: String = "Oliver", avatar: Character.Avatar? = null) =
        (SaveCharacter.parse(name, "Curious", "Green eyes", "CHILD", avatar) as SaveCharacter.Result.Valid).command

    @Test fun replacesWithoutDuplicatesIsolatesOwnersAndPermanentlyReservesDeletedIdsAfterRestart() = runBlocking {
        database().use { db ->
            val repository = SqliteCharacterRepository(db)
            assertEquals(SaveCharacterResult.Created, repository.save(owner, id, command()))
            repeat(2) { assertEquals(SaveCharacterResult.Updated, repository.save(owner, id, command("Edited"))) }
            assertEquals("Edited", repository.list(owner).single().name)
            assertTrue(repository.list(stranger).isEmpty())
            assertEquals(SaveCharacterResult.Conflict, repository.save(stranger, id, command()))
            assertFalse(repository.delete(stranger, id))
            assertFalse(repository.delete(owner, Character.Id("missing")))
            repeat(2) { assertTrue(repository.delete(owner, id)) }
            assertTrue(repository.list(owner).isEmpty())
        }
        database().use { db ->
            val repository = SqliteCharacterRepository(db)
            assertTrue(repository.delete(owner, id))
            assertEquals(SaveCharacterResult.Conflict, repository.save(owner, id, command()))
            assertEquals(SaveCharacterResult.Conflict, repository.save(stranger, id, command()))
        }
    }

    @Test fun onlyOwnedReadyAssetWithExactIdAndUrlCanBeReferencedAndReplacementClearsAvatar() = runBlocking {
        database().use { db ->
            db.write {
                registerInstallation(owner)
                registerInstallation(stranger)
                for ((assetId, installation, state) in listOf(Triple("ready", owner, "READY"), Triple("pending", owner, "PENDING"), Triple("failed", owner, "FAILED"), Triple("foreign", stranger, "READY"))) {
                    Assets.insert { it[id] = assetId; it[installationId] = installation.value; it[url] = "https://example.test/$assetId"; it[status] = state }
                }
            }
            val repository = SqliteCharacterRepository(db)
            for (assetId in listOf("missing", "foreign", "pending", "failed")) {
                assertEquals(SaveCharacterResult.ReferenceUnavailable, repository.save(owner, id, command(avatar = Character.Avatar(assetId, "https://example.test/$assetId"))))
                assertTrue(repository.list(owner).isEmpty())
            }
            assertEquals(SaveCharacterResult.ReferenceUnavailable, repository.save(owner, id, command(avatar = Character.Avatar("ready", "https://example.test/wrong"))))
            val avatar = Character.Avatar("ready", "https://example.test/ready")
            assertEquals(SaveCharacterResult.Created, repository.save(owner, id, command(avatar = avatar)))
            assertEquals(avatar, repository.list(owner).single().avatar)
        }
        database().use { db ->
            val repository = SqliteCharacterRepository(db)
            assertEquals("ready", repository.list(owner).single().avatar?.id)
            assertEquals(SaveCharacterResult.Updated, repository.save(owner, id, command()))
            assertEquals(null, repository.list(owner).single().avatar)
            db.write { Characters.update({ Characters.id eq this@SqliteCharacterRepositoryTest.id.value }) { it[name] = "x".repeat(70); it[personality] = "" } }
            assertEquals("x".repeat(70), repository.list(owner).single().name)
            assertEquals("", repository.list(owner).single().personality)
        }
    }

    @Test fun concurrentClaimsHaveOnlyOneOwner() = runBlocking {
        database().use { first -> database().use { second ->
            val results = listOf(first to owner, second to stranger).map { (db, installation) ->
                async { SqliteCharacterRepository(db).save(installation, id, command()) }
            }.awaitAll()
            assertEquals(setOf(SaveCharacterResult.Created, SaveCharacterResult.Conflict), results.toSet())
        } }
    }
}
