package com.dozy.features.place.api

import com.dozy.databaseConfig
import com.dozy.features.installation.domain.model.InstallationId
import com.dozy.features.place.domain.model.Place
import com.dozy.features.place.domain.model.SavePlace
import com.dozy.features.place.domain.model.SavePlaceResult
import com.dozy.plugins.FieldLengthErrorResponse
import com.dozy.plugins.FieldValidationErrorResponse
import com.dozy.features.place.domain.repository.PlaceRepository
import com.dozy.module
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.plugins.di.dependencies
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import java.nio.file.Files
import java.nio.file.Path
import java.sql.SQLException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.io.TempDir

class PlaceRoutesTest {
    @TempDir lateinit var directory: Path
    private val owner = "019a0000-0000-7000-8000-000000000001"
    private val stranger = "019a0000-0000-7000-8000-000000000002"
    private val id = "019a0000-0000-7000-8000-000000000003"

    @Test
    fun `lists builtins then saves and replaces a normalized custom place`() = testApplication {
        configurePlacesTest()
        val initial = client.places()
        assertEquals(3, initial.size)
        assertEquals(setOf("builtin:village", "builtin:island", "builtin:woods"), initial.map { it.id }.toSet())
        initial.forEach { assertFalse(it.editable); assertEquals("PREDEFINED", it.ownership) }
        val created = client.save(body = body(name = "  Garden  ", inhabitants = "Owls"))
        assertEquals(HttpStatusCode.Created, created.status)
        assertEquals("", created.bodyAsText())
        val saved = client.places().first()
        assertEquals(id, saved.id)
        assertEquals("Garden", saved.name)
        assertEquals("CUSTOM", saved.ownership)
        assertEquals(true, saved.editable)
        assertEquals("Owls", saved.inhabitants)
        val updated = client.save()
        assertEquals(HttpStatusCode.NoContent, updated.status)
        assertEquals("", updated.bodyAsText())
        val owned = client.places()
        assertEquals(4, owned.size)
        assertNull(owned.first().inhabitants)
        assertEquals(HttpStatusCode.NoContent, client.save().status)
        assertEquals(4, client.places().size)
    }

    @Test
    fun `validates installation header for reads and writes`() = testApplication {
        configurePlacesTest()
        for (invalid in listOf(null, "broken", "1-1-1-1-1", "$owner,$stranger")) {
            val get = client.get("/places") { invalid?.let { header("X-Installation-Id", it) } }
            assertError(get, HttpStatusCode.BadRequest, "INVALID_INSTALLATION_ID")
            assertError(client.save(installation = invalid), HttpStatusCode.BadRequest, "INVALID_INSTALLATION_ID")
        }
        val duplicate = client.get("/places") {
            header("X-Installation-Id", owner)
            header("X-Installation-Id", stranger)
        }
        assertError(duplicate, HttpStatusCode.BadRequest, "INVALID_INSTALLATION_ID")
    }

    @Test
    fun `accepts canonical UUID case and retains ownership isolation`() = testApplication {
        configurePlacesTest()
        val uppercase = "019A0000-0000-7000-8000-00000000000A"
        assertEquals(HttpStatusCode.Created, client.save(installation = uppercase).status)
        assertEquals(4, client.places(uppercase.lowercase()).size)
        assertEquals(3, client.places(stranger).size)
        assertError(client.save(installation = stranger), HttpStatusCode.Conflict, "CONFLICT")
        assertEquals(4, client.places(uppercase).size)
    }

    @Test
    fun `protects predefined namespace even for unknown IDs`() = testApplication {
        configurePlacesTest()
        for (builtin in listOf("builtin:woods", "builtin:missing")) {
            assertError(client.save(path = builtin, body = body(resourceId = builtin)), HttpStatusCode.Forbidden, "FORBIDDEN")
        }
        assertEquals("Whispering woods", client.places().single { it.id == "builtin:woods" }.name)
    }

    @Test
    fun `rejects mismatched identity flags malformed JSON and missing fields`() = testApplication {
        configurePlacesTest()
        val invalidBodies = listOf(
            body(resourceId = stranger), body().replace("CUSTOM", "PREDEFINED"),
            body().replace("true", "false"), "{", "{}",
        )
        for (invalid in invalidBodies) {
            assertError(client.save(body = invalid), HttpStatusCode.BadRequest, "INVALID_REQUEST")
        }
        assertEquals(3, client.places().size)
    }

    @Test
    fun `invalid business input does not persist a place`() = testApplication {
        configurePlacesTest()
        val invalidInputs = listOf(
            body(name = " ") to ("name" to "REQUIRED"),
            body(name = "n".repeat(81)) to ("name" to "TOO_LONG"),
            body(description = " ") to ("description" to "REQUIRED"),
            body(description = "d".repeat(501)) to ("description" to "TOO_LONG"),
            body(inhabitants = "i".repeat(501)) to ("inhabitants" to "TOO_LONG"),
        )
        for ((invalid, expected) in invalidInputs) {
            val response = client.save(body = invalid)
            val maxLength = if (expected.second == "TOO_LONG") {
                if (expected.first == "name") 80 else 500
            } else null
            assertFieldError(response, expected.first, expected.second, maxLength)
        }
        assertEquals(3, client.places().size)
    }

    @Test
    fun `DI fake overrides production repository and database failures have safe errors`() = testApplication {
        environment { config = databaseConfig(directory) }
        application {
            dependencies.provide<PlaceRepository> { FailingRepository() }
            module()
        }
        assertError(client.get("/places") { header("X-Installation-Id", owner) },
            HttpStatusCode.ServiceUnavailable, "UNAVAILABLE")
        assertError(client.save(), HttpStatusCode.ServiceUnavailable, "UNAVAILABLE")
        assertFalse(Files.exists(directory.resolve("dozy.db")))
    }

    private fun ApplicationTestBuilder.configurePlacesTest() {
        environment { config = databaseConfig(directory) }
        application { module() }
    }

    private suspend fun HttpClient.places(installation: String = owner): List<PlaceResponse> {
        val response = get("/places") { header("X-Installation-Id", installation) }
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("application/json", response.headers["Content-Type"]?.substringBefore(';'))
        return Json.decodeFromString<PlacesResponse>(response.bodyAsText()).places
    }

    private suspend fun HttpClient.save(
        installation: String? = owner,
        path: String = id,
        body: String = body(),
    ) = put("/places/$path") {
        installation?.let { header("X-Installation-Id", it) }
        contentType(ContentType.Application.Json)
        setBody(body)
    }

    private fun body(resourceId: String = id, name: String = "Garden", description: String = "Quiet garden", inhabitants: String? = null) =
        Json.encodeToString(SavePlaceRequest(resourceId, name, description, "CUSTOM", true, inhabitants))

    private suspend fun assertError(response: HttpResponse, status: HttpStatusCode, code: String) {
        assertEquals(status, response.status)
        assertEquals("""{"code":"$code"}""", response.bodyAsText())
    }

    private suspend fun assertFieldError(response: HttpResponse, field: String, reason: String, maxLength: Int?) {
        assertEquals(HttpStatusCode.UnprocessableEntity, response.status)
        if (maxLength == null) {
            val error = Json.decodeFromString<FieldValidationErrorResponse>(response.bodyAsText())
            assertEquals("INVALID_REQUEST", error.code)
            assertEquals(field, error.field)
            assertEquals(reason, error.reason)
        } else {
            val error = Json.decodeFromString<FieldLengthErrorResponse>(response.bodyAsText())
            assertEquals("INVALID_REQUEST", error.code)
            assertEquals(field, error.field)
            assertEquals(reason, error.reason)
            assertEquals(maxLength, error.maxLength)
        }
    }

    private class FailingRepository : PlaceRepository {
        override suspend fun list(installation: InstallationId): List<Place> = throw SQLException("Private database path")
        override suspend fun save(installation: InstallationId, id: Place.Id, command: SavePlace): SavePlaceResult =
            throw SQLException("Private database path")
    }
}
