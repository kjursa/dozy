package com.dozy.features.place.data.repository

import com.dozy.config.DatabaseConfig
import com.dozy.domain.id.OwnerId
import com.dozy.domain.id.PlaceId
import com.dozy.features.place.domain.model.ParseResult
import com.dozy.features.place.domain.model.Place
import com.dozy.features.place.domain.model.SavePlace
import com.dozy.features.place.domain.model.SavePlaceResult
import com.dozy.infrastructure.database.OwnerPartitions
import com.dozy.infrastructure.database.SqliteDatabase
import com.dozy.infrastructure.database.registerOwnerPartition
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull

class SqlitePlaceRepositoryTest {
    @TempDir lateinit var directory: Path
    private val databases = mutableListOf<SqliteDatabase>()
    private val owner = OwnerId.generate()
    private val stranger = OwnerId.generate()
    private val input = savePlace("Woods", "Forest", "Owls")

    @Test
    fun `seeds shared catalog and isolates custom data by ownerId`() = runBlocking {
        val repository = repository()
        val id = assertIs<SavePlaceResult.Created>(repository.create(owner, input)).id
        val owned = repository.list(owner)
        assertEquals(4, owned.size)
        assertEquals("Owls", owned.single { it.id == id }.inhabitants?.value)
        assertEquals(
            setOf(
                "place-00000000-0000-7000-8000-000000000001",
                "place-00000000-0000-7000-8000-000000000002",
                "place-00000000-0000-7000-8000-000000000003",
            ),
            repository.list(stranger).map { it.id.value }.toSet(),
        )
    }

    @Test
    fun `repeat saves replace one resource and omitted inhabitants clears previous value`() =
        runBlocking {
            val repository = repository()
            val id = assertIs<SavePlaceResult.Created>(repository.create(owner, input)).id
            val replacement = savePlace("Island", "Sea", null)
            repeat(2) {
                assertEquals(SavePlaceResult.Updated, repository.save(owner, id, replacement))
            }
            val custom = repository.list(owner).filter { it.origin == Place.Origin.USER }
            assertEquals(1, custom.size)
            assertEquals("Island", custom.single().name.value)
            assertNull(custom.single().inhabitants)
        }

    @Test
    fun `rejects another owner and predefined edits without changing records`() = runBlocking {
        val repository = repository()
        val id = assertIs<SavePlaceResult.Created>(repository.create(owner, input)).id
        assertEquals(SavePlaceResult.Conflict, repository.save(stranger, id, input))
        assertEquals(SavePlaceResult.Forbidden, repository.save(owner, placeId("place-00000000-0000-7000-8000-000000000001"), input))
        assertEquals(SavePlaceResult.NotFound, repository.save(owner, placeId("place-00000000-0000-7000-8000-000000000004"), input))
        assertEquals("Whispering woods", repository.list(owner).single { it.id.value == "place-00000000-0000-7000-8000-000000000001" }.name.value)
        assertFalse(repository.list(stranger).any { it.id == id })
    }

    @Test
    fun `survives reopening and migration does not duplicate seeds`() = runBlocking {
        val id = assertIs<SavePlaceResult.Created>(repository().create(owner, input)).id
        val reopened = repository().list(owner)
        assertEquals(4, reopened.size)
        assertEquals("Woods", reopened.single { it.id == id }.name.value)
    }

    @Test
    fun `concurrent creates have distinct IDs and isolated owners`() = runBlocking {
        val repositories = listOf(repository() to owner, repository() to stranger)
        val ids = repositories.map { (repository, ownerId) ->
            async { assertIs<SavePlaceResult.Created>(repository.create(ownerId, input)).id }
        }.awaitAll()
        assertEquals(2, ids.distinct().size)
        repositories.zip(ids).forEach { (entry, id) ->
            assertEquals(
                id,
                entry.first.list(entry.second).single {
                    it.origin == Place.Origin.USER
                }.id
            )
        }
    }

    @Test
    fun `failed transaction rolls back all writes`() = runBlocking {
        val database = database()
        val ownerId = OwnerId.generate()
        assertFailsWith<IllegalStateException> {
            database.write {
                registerOwnerPartition(ownerId)
                error("Rollback")
            }
        }
        val count = database.write {
            OwnerPartitions.selectAll().where { OwnerPartitions.id eq ownerId.value }.count()
        }
        assertEquals(0L, count)
    }

    @Test
    fun `propagates coroutine cancellation`() = runBlocking<Unit> {
        assertFailsWith<CancellationException> { database().write { throw CancellationException("Cancelled") } }
    }

    @Test
    fun `round trips quoted and Unicode input through DSL`() = runBlocking {
        val repository = repository()
        val special = savePlace("O'Brien's 🌙 garden", "A forest; with 'quoted' paths", "Owls 🦉")
        val id = assertIs<SavePlaceResult.Created>(repository.create(owner, special)).id
        val saved = repository.list(owner).single { it.id == id }
        assertEquals(special.name, saved.name)
        assertEquals(special.description, saved.description)
        assertEquals(special.inhabitants, saved.inhabitants)
    }

    @AfterEach
    fun closeDatabases() {
        databases.forEach(SqliteDatabase::close)
    }

    private fun database() = SqliteDatabase(DatabaseConfig(directory.resolve("nested/dozy.db").toString(), 5000))
        .also(databases::add)
    private fun repository() = SqlitePlaceRepository(database())
    private fun placeId(value: String) = requireNotNull(PlaceId.parse(value))
    private fun savePlace(name: String, description: String, inhabitants: String?) =
        (SavePlace.parse(name, description, inhabitants) as ParseResult.Valid).value
}
