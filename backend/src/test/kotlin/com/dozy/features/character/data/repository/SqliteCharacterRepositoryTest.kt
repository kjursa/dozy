package com.dozy.features.character.data.repository

import com.dozy.config.DatabaseConfig
import com.dozy.domain.id.AssetId
import com.dozy.domain.id.CharacterId
import com.dozy.domain.id.OwnerId
import com.dozy.features.asset.data.database.Assets
import com.dozy.features.character.data.database.CharacterDao
import com.dozy.features.character.data.database.Characters
import com.dozy.features.character.domain.model.Character
import com.dozy.features.character.domain.model.SaveCharacter
import com.dozy.features.character.domain.model.SaveCharacterResult
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
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class SqliteCharacterRepositoryTest {
    @TempDir lateinit var directory: Path
    private val owner = OwnerId.generate()
    private val stranger = OwnerId.generate()
    private fun database() = SqliteDatabase(DatabaseConfig(directory.resolve("dozy.db").toString(), 5000))
    private fun command(name: String = "Oliver", avatar: Character.Avatar? = null) =
        (SaveCharacter.parse(name, "Curious", "Green eyes", "CHILD", avatar) as SaveCharacter.Result.Valid).command

    @Test fun replacesWithoutDuplicatesIsolatesOwnersAndPermanentlyReservesDeletedIdsAfterRestart() = runBlocking {
        val id = database().use { db ->
            val repository = SqliteCharacterRepository(db, CharacterDao(db))
            val id = assertIs<SaveCharacterResult.Created>(repository.create(owner, command())).id
            repeat(2) { assertEquals(SaveCharacterResult.Updated, repository.save(owner, id, command("Edited"))) }
            assertEquals("Edited", repository.list(owner).single().name)
            assertTrue(repository.list(stranger).isEmpty())
            assertEquals(SaveCharacterResult.Conflict, repository.save(stranger, id, command()))
            assertFalse(repository.delete(stranger, id))
            assertFalse(repository.delete(owner, CharacterId.generate()))
            repeat(2) { assertTrue(repository.delete(owner, id)) }
            assertTrue(repository.list(owner).isEmpty())
            id
        }
        database().use { db ->
            val repository = SqliteCharacterRepository(db, CharacterDao(db))
            assertTrue(repository.delete(owner, id))
            assertEquals(SaveCharacterResult.Conflict, repository.save(owner, id, command()))
            assertEquals(SaveCharacterResult.Conflict, repository.save(stranger, id, command()))
        }
    }

    @Test fun onlyOwnedReadyAssetWithExactIdAndUrlCanBeReferencedAndReplacementClearsAvatar() = runBlocking {
        database().use { db ->
            val assets = listOf("ready", "pending", "failed", "foreign", "missing").associateWith { AssetId.generate() }
            db.write {
                registerOwnerPartition(owner)
                registerOwnerPartition(stranger)
                for ((assetId, ownerId, state) in listOf(Triple("ready", owner, "READY"), Triple("pending", owner, "PENDING"), Triple("failed", owner, "FAILED"), Triple("foreign", stranger, "READY"))) {
                    Assets.insert { it[id] = assets.getValue(assetId).value; it[Assets.ownerId] = ownerId.value; it[url] = "https://example.test/$assetId"; it[status] = state }
                }
            }
            val repository = SqliteCharacterRepository(db, CharacterDao(db))
            for (assetId in listOf("missing", "foreign", "pending", "failed")) {
                assertEquals(SaveCharacterResult.ReferenceUnavailable, repository.create(owner, command(avatar = Character.Avatar(assets.getValue(assetId), "https://example.test/$assetId"))))
                assertTrue(repository.list(owner).isEmpty())
            }
            assertEquals(SaveCharacterResult.ReferenceUnavailable, repository.create(owner, command(avatar = Character.Avatar(assets.getValue("ready"), "https://example.test/wrong"))))
            val avatar = Character.Avatar(assets.getValue("ready"), "https://example.test/ready")
            assertIs<SaveCharacterResult.Created>(repository.create(owner, command(avatar = avatar)))
            assertEquals(avatar, repository.list(owner).single().avatar)
        }
        database().use { db ->
            val repository = SqliteCharacterRepository(db, CharacterDao(db))
            val saved = repository.list(owner).single()
            val id = saved.id
            assertEquals("https://example.test/ready", saved.avatar?.url)
            assertEquals(SaveCharacterResult.Updated, repository.save(owner, id, command()))
            assertEquals(null, repository.list(owner).single().avatar)
            db.write { Characters.update({ Characters.id eq id.value }) { it[name] = "x".repeat(70); it[personality] = "" } }
            assertEquals("x".repeat(70), repository.list(owner).single().name)
            assertEquals("", repository.list(owner).single().personality)
        }
    }

    @Test fun concurrentCreatesHaveDistinctIdsAndIsolatedOwners() = runBlocking {
        database().use { first ->
            database().use { second ->
                val repositories = listOf(SqliteCharacterRepository(first, CharacterDao(first)) to owner, SqliteCharacterRepository(second, CharacterDao(second)) to stranger)
                val ids = repositories.map { (repository, ownerId) ->
                    async { assertIs<SaveCharacterResult.Created>(repository.create(ownerId, command())).id }
                }.awaitAll()
                assertEquals(2, ids.distinct().size)
                repositories.zip(ids).forEach { (entry, id) ->
                    assertEquals(id, entry.first.list(entry.second).single().id)
                }
            }
        }
    }
}
