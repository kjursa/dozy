package com.dozy.features.place.data.repository

import com.dozy.config.DatabaseConfig
import com.dozy.features.installation.domain.model.InstallationId
import com.dozy.features.place.domain.model.Place
import com.dozy.features.place.domain.model.PlaceInput
import com.dozy.features.place.domain.model.SavePlaceResult
import com.dozy.infrastructure.database.SqliteDatabase
import java.nio.file.Path
import java.sql.DriverManager
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.io.TempDir

class SqlitePlaceRepositoryTest {
    @TempDir lateinit var directory: Path
    private val owner = InstallationId("owner")
    private val stranger = InstallationId("stranger")
    private val id = Place.Id("019a0000-0000-7000-8000-000000000001")
    private val input = requireNotNull(PlaceInput.from("Woods", "Forest", "Owls"))

    @Test
    fun `seeds shared catalog and isolates custom data by installation`() = runBlocking {
        val repository = repository()
        assertEquals(SavePlaceResult.Created, repository.save(owner, id, input))
        val owned = repository.list(owner)
        assertEquals(4, owned.size)
        assertEquals("Owls", owned.single { it.id == id }.inhabitants)
        assertEquals(setOf("builtin:woods", "builtin:island", "builtin:village"),
            repository.list(stranger).map { it.id.value }.toSet())
    }

    @Test
    fun `repeat saves replace one resource and omitted inhabitants clears previous value`() = runBlocking {
        val repository = repository()
        repository.save(owner, id, input)
        val replacement = requireNotNull(PlaceInput.from("Island", "Sea", null))
        repeat(2) { assertEquals(SavePlaceResult.Updated, repository.save(owner, id, replacement)) }
        val custom = repository.list(owner).filter { it.ownership == Place.Ownership.CUSTOM }
        assertEquals(1, custom.size)
        assertEquals("Island", custom.single().name)
        assertNull(custom.single().inhabitants)
    }

    @Test
    fun `rejects another owner and predefined edits without changing records`() = runBlocking {
        val repository = repository()
        repository.save(owner, id, input)
        assertEquals(SavePlaceResult.Conflict, repository.save(stranger, id, input))
        assertEquals(SavePlaceResult.Forbidden, repository.save(owner, Place.Id("builtin:woods"), input))
        assertEquals(SavePlaceResult.Forbidden, repository.save(owner, Place.Id("builtin:missing"), input))
        assertEquals("Whispering woods", repository.list(owner).single { it.id.value == "builtin:woods" }.name)
        assertFalse(repository.list(stranger).any { it.id == id })
    }

    @Test
    fun `survives reopening and migration does not duplicate seeds`() = runBlocking {
        repository().save(owner, id, input)
        val reopened = repository().list(owner)
        assertEquals(4, reopened.size)
        assertEquals("Woods", reopened.single { it.id == id }.name)
    }

    @Test
    fun `concurrent claims cannot transfer ownership`() = runBlocking {
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
            database.write { connection ->
                connection.createStatement().use { it.execute("INSERT INTO installations(id) VALUES ('rollback')") }
                error("Rollback")
            }
        }
        val count = database.write { connection ->
            connection.createStatement().use { statement ->
                statement.executeQuery("SELECT count(*) FROM installations WHERE id = 'rollback'").use {
                    it.next()
                    it.getInt(1)
                }
            }
        }
        assertEquals(0, count)
    }

    @Test
    fun `rejects unknown schema version without overwriting it`() {
        val path = directory.resolve("future.db")
        DriverManager.getConnection("jdbc:sqlite:$path").use { connection ->
            connection.createStatement().use { it.execute("PRAGMA user_version = 2") }
        }
        assertFailsWith<IllegalStateException> { SqliteDatabase(DatabaseConfig(path.toString(), 5000)) }
        DriverManager.getConnection("jdbc:sqlite:$path").use { connection ->
            connection.createStatement().use { statement ->
                statement.executeQuery("PRAGMA user_version").use {
                    it.next()
                    assertEquals(2, it.getInt(1))
                }
            }
        }
    }

    @Test
    fun `propagates coroutine cancellation`() = runBlocking<Unit> {
        assertFailsWith<CancellationException> { database().write { throw CancellationException("Cancelled") } }
    }

    private fun database() = SqliteDatabase(DatabaseConfig(directory.resolve("nested/dozy.db").toString(), 5000))
    private fun repository() = SqlitePlaceRepository(database())
}
