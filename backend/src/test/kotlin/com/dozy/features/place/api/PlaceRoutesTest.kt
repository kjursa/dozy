package com.dozy.features.place.api

import com.dozy.api.contracts.v1.ApiErrorResponse
import com.dozy.api.contracts.v1.place.PlaceResponse
import com.dozy.api.contracts.v1.place.PlacesResponse
import com.dozy.api.contracts.v1.place.SavePlaceRequest
import com.dozy.databaseConfig
import com.dozy.features.installation.domain.model.InstallationId
import com.dozy.features.place.domain.model.Place
import com.dozy.features.place.domain.model.SavePlace
import com.dozy.features.place.domain.model.SavePlaceResult
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
import kotlinx.serialization.json.jsonObject
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
        initial.forEach { assertFalse(it.editable); assertEquals("SYSTEM", it.origin) }
        val created = client.save(body = body(name = "  Garden  ", inhabitants = "Owls"))
        assertEquals(HttpStatusCode.Created, created.status)
        assertEquals("", created.bodyAsText())
        val saved = client.places().first()
        assertEquals(id, saved.id)
        assertEquals("Garden", saved.name)
        assertEquals("USER", saved.origin)
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
            val get = client.get("/api/v1/places") { invalid?.let { header("X-Installation-Id", it) } }
            assertError(get, HttpStatusCode.BadRequest, "INVALID_INSTALLATION_ID")
            assertError(client.save(installation = invalid), HttpStatusCode.BadRequest, "INVALID_INSTALLATION_ID")
        }
        val duplicate = client.get("/api/v1/places") {
            header("X-Installation-Id", owner)
            header("X-Installation-Id", stranger)
        }
        assertError(duplicate, HttpStatusCode.BadRequest, "INVALID_INSTALLATION_ID")
    }

    @Test
    fun `accepts canonical UUID case and retains origin isolation`() = testApplication {
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
            assertError(client.save(path = builtin, body = body()), HttpStatusCode.Forbidden, "FORBIDDEN")
        }
        assertEquals("Whispering woods", client.places().single { it.id == "builtin:woods" }.name)
    }

    @Test
    fun `rejects malformed JSON and missing fields`() = testApplication {
        configurePlacesTest()
        val invalidBodies = listOf(
            "{", "{}", """{"name":"Garden"}""",
            """{"name":"Garden","description":"Quiet","origin":"SYSTEM","editable":true}""",
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
            assertFieldError(response, expected.first, expected.second)
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
        assertError(client.get("/api/v1/places") { header("X-Installation-Id", owner) },
            HttpStatusCode.ServiceUnavailable, "UNAVAILABLE")
        assertError(client.save(), HttpStatusCode.ServiceUnavailable, "UNAVAILABLE")
        assertFalse(Files.exists(directory.resolve("dozy.db")))
    }

    private fun ApplicationTestBuilder.configurePlacesTest() {
        environment { config = databaseConfig(directory) }
        application { module() }
    }

    private suspend fun HttpClient.places(installation: String = owner): List<PlaceResponse> {
        val response = get("/api/v1/places") { header("X-Installation-Id", installation) }
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("application/json", response.headers["Content-Type"]?.substringBefore(';'))
        return Json.decodeFromString<PlacesResponse>(response.bodyAsText()).places
    }

    private suspend fun HttpClient.save(
        installation: String? = owner,
        path: String = id,
        body: String = body(),
    ) = put("/api/v1/places/$path") {
        installation?.let { header("X-Installation-Id", it) }
        contentType(ContentType.Application.Json)
        setBody(body)
    }

    private fun body(name: String = "Garden", description: String = "Quiet garden", inhabitants: String? = null) =
        Json.encodeToString(SavePlaceRequest(name, description, inhabitants))

    private suspend fun assertError(response: HttpResponse, status: HttpStatusCode, code: String) {
        assertEquals(status, response.status)
        val error = decodeError(response)
        assertEquals(code, error.code)
        assertEquals(null, error.field)
        assertEquals(null, error.reason)
    }

    private suspend fun assertFieldError(response: HttpResponse, field: String, reason: String) {
        assertEquals(HttpStatusCode.UnprocessableEntity, response.status)
        val error = decodeError(response)
        assertEquals("INVALID_REQUEST", error.code)
        assertEquals(field, error.field)
        assertEquals(reason, error.reason)
    }

    private suspend fun decodeError(response: HttpResponse): ApiErrorResponse {
        val body = response.bodyAsText()
        assertEquals(setOf("code", "field", "reason"), Json.parseToJsonElement(body).jsonObject.keys)
        return Json.decodeFromString(body)
    }

    private class FailingRepository : PlaceRepository {
        override suspend fun list(installation: InstallationId): List<Place> = throw SQLException("Private database path")
        override suspend fun save(installation: InstallationId, id: Place.Id, command: SavePlace): SavePlaceResult =
            throw SQLException("Private database path")
    }
}
