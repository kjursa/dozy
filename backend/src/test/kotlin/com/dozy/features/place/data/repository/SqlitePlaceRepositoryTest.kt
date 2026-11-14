package com.dozy.features.place.data.repository

import com.dozy.config.DatabaseConfig
import com.dozy.features.installation.data.database.Installations
import com.dozy.features.installation.data.database.registerInstallation
import com.dozy.features.installation.domain.model.InstallationId
import com.dozy.features.place.domain.model.Place
import com.dozy.features.place.domain.model.ParseResult
import com.dozy.features.place.domain.model.SavePlace
import com.dozy.features.place.domain.model.SavePlaceResult
import com.dozy.infrastructure.database.SqliteDatabase
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.io.TempDir

class SqlitePlaceRepositoryTest {
    @TempDir lateinit var directory: Path
    private val databases = mutableListOf<SqliteDatabase>()
    private val owner = InstallationId("owner")
    private val stranger = InstallationId("stranger")
    private val id = placeId("019a0000-0000-7000-8000-000000000001")
    private val input = savePlace("Woods", "Forest", "Owls")

    @Test
    fun `seeds shared catalog and isolates custom data by installation`() = runBlocking {
        val repository = repository()
        assertEquals(SavePlaceResult.Created, repository.save(owner, id, input))
        val owned = repository.list(owner)
        assertEquals(4, owned.size)
        assertEquals("Owls", owned.single { it.id == id }.inhabitants?.value)
        assertEquals(setOf("builtin:woods", "builtin:island", "builtin:village"),
            repository.list(stranger).map { it.id.value }.toSet())
    }

    @Test
    fun `repeat saves replace one resource and omitted inhabitants clears previous value`() = runBlocking {
        val repository = repository()
        repository.save(owner, id, input)
        val replacement = savePlace("Island", "Sea", null)
        repeat(2) { assertEquals(SavePlaceResult.Updated, repository.save(owner, id, replacement)) }
        val custom = repository.list(owner).filter { it.origin == Place.Origin.USER }
        assertEquals(1, custom.size)
        assertEquals("Island", custom.single().name.value)
        assertNull(custom.single().inhabitants)
    }

    @Test
    fun `rejects another owner and predefined edits without changing records`() = runBlocking {
        val repository = repository()
        repository.save(owner, id, input)
        assertEquals(SavePlaceResult.Conflict, repository.save(stranger, id, input))
        assertEquals(SavePlaceResult.Forbidden, repository.save(owner, placeId("builtin:woods"), input))
        assertEquals(SavePlaceResult.Forbidden, repository.save(owner, placeId("builtin:missing"), input))
        assertEquals("Whispering woods", repository.list(owner).single { it.id.value == "builtin:woods" }.name.value)
        assertFalse(repository.list(stranger).any { it.id == id })
    }

    @Test
    fun `survives reopening and migration does not duplicate seeds`() = runBlocking {
        repository().save(owner, id, input)
        val reopened = repository().list(owner)
        assertEquals(4, reopened.size)
        assertEquals("Woods", reopened.single { it.id == id }.name.value)
    }

    @Test
    fun `concurrent claims cannot transfer origin`() = runBlocking {
        val repositories = listOf(repository(), repository())
        val results = listOf(owner, stranger).mapIndexed { index, installation ->
            async { repositories[index].save(installation, id, input) }
        }.awaitAll()
        assertEquals(setOf(SavePlaceResult.Created, SavePlaceResult.Conflict), results.toSet())
        assertEquals(1, listOf(owner, stranger).sumOf { installation ->
            repositories.first().list(installation).count { it.id == id }
        })
    }

    @Test
    fun `failed transaction rolls back all writes`() = runBlocking {
        val database = database()
        assertFailsWith<IllegalStateException> {
            database.write {
                registerInstallation(InstallationId("rollback"))
                error("Rollback")
            }
        }
        val count = database.write {
            Installations.selectAll().where { Installations.id eq "rollback" }.count()
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
        repository.save(owner, id, special)
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
    private fun placeId(value: String) = Place.Id(value)
    private fun savePlace(name: String, description: String, inhabitants: String?) =
        (SavePlace.parse(name, description, inhabitants) as ParseResult.Valid).value
}
