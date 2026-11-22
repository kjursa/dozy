package com.dozy.features.character.api

import com.dozy.api.contracts.v1.ApiErrorResponse
import com.dozy.api.contracts.v1.character.CharacterDto
import com.dozy.api.contracts.v1.character.CharactersSaveError
import com.dozy.api.contracts.v1.character.CreatedCharacterResponse
import com.dozy.api.contracts.v1.character.SaveCharacterRequest
import com.dozy.asOwner
import com.dozy.databaseConfig
import com.dozy.domain.id.OwnerId
import com.dozy.module
import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CharacterRoutesTest {
    @TempDir lateinit var directory: Path
    private val owner = requireNotNull(OwnerId.parse("owner-019a0000-0000-7000-8000-000000000001"))
    private val stranger = requireNotNull(OwnerId.parse("owner-019a0000-0000-7000-8000-000000000002"))
    private val collection = "/api/v1/characters"
    private val input = SaveCharacterRequest("  Oliver  ", "Curious", "Green eyes", "CHILD")
    private suspend fun HttpClient.create(
        body: String = Json.encodeToString(input),
        requestKey: String? = null
    ) = post(collection) {
        asOwner(owner)
        contentType(ContentType.Application.Json)
        requestKey?.let { header("Idempotency-Key", it) }
        setBody(body)
    }
    private suspend fun HttpClient.save(id: String, ownerId: OwnerId = owner) =
        put("$collection/$id") {
            asOwner(ownerId)
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(input))
        }
    private suspend fun HttpClient.list(ownerId: OwnerId = owner): List<CharacterDto> {
        val response = get("/api/v1/characters") { asOwner(ownerId) }
        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().startsWith("["))
        return Json.decodeFromString(response.bodyAsText())
    }

    @Test fun `characters validators revalidate only the current collection`() =
        testApplication {
            environment { config = databaseConfig(directory) }
            application { module() }

            suspend fun revalidate(
                ownerId: OwnerId,
                tag: String,
            ) = client.get(collection) {
                asOwner(ownerId)
                header(HttpHeaders.IfNoneMatch, tag)
            }
            val initial = client.get(collection) { asOwner(owner) }
            val initialTag = requireNotNull(initial.headers[HttpHeaders.ETag])
            val unchanged = revalidate(owner, "\"other\", ${initialTag.removePrefix("W/")}")
            assertEquals(HttpStatusCode.NotModified, unchanged.status)
            assertEquals(initialTag, unchanged.headers[HttpHeaders.ETag])
            assertEquals("", unchanged.bodyAsText())

            assertEquals(HttpStatusCode.Created, client.create().status)
            val changed = revalidate(owner, initialTag)
            assertEquals(HttpStatusCode.OK, changed.status)
            val changedTag = requireNotNull(changed.headers[HttpHeaders.ETag])
            assertEquals(HttpStatusCode.OK, revalidate(stranger, changedTag).status)
            assertEquals(HttpStatusCode.NotModified, revalidate(owner, changedTag).status)
            val removed = client.delete("$collection/${client.list().single().id}") { asOwner(owner) }
            assertEquals(HttpStatusCode.NoContent, removed.status)
            assertEquals(HttpStatusCode.OK, revalidate(owner, changedTag).status)
            assertEquals(HttpStatusCode.NotModified, revalidate(owner, initialTag).status)
        }

    @Test fun createsWithServerIdThenUpdatesAndPermanentlyDeletes() = testApplication {
        environment { config = databaseConfig(directory) }
        application { module() }
        assertEquals(emptyList(), client.list())
        val minimal = """{"name":" Oliver ","personality":"Curious","appearance":"Green eyes","type":"CHILD"}"""
        val missingId = "char-019a0000-0000-7000-8000-000000000003"
        assertEquals(HttpStatusCode.NotFound, client.save(missingId).status)
        assertTrue(client.list().isEmpty())
        val created = client.create(minimal)
        assertEquals(HttpStatusCode.Created, created.status)
        val id = Json.decodeFromString<CreatedCharacterResponse>(created.bodyAsText()).id
        assertTrue(id.startsWith("char-"))
        assertEquals(7, UUID.fromString(id.removePrefix("char-")).version())
        assertEquals(id, client.list().single().id)
        val path = "$collection/$id"
        assertEquals("Oliver", client.list().single().name)
        assertEquals(null, client.list().single().avatar)
        repeat(2) {
            val updated = client.save(id)
            assertEquals(HttpStatusCode.NoContent, updated.status)
            assertEquals("", updated.bodyAsText())
        }
        assertEquals(1, client.list().size)
        assertTrue(client.list(stranger).isEmpty())
        val conflict = client.save(id, ownerId = stranger)
        assertEquals(HttpStatusCode.Conflict, conflict.status)
        assertEquals("CONFLICT", Json.decodeFromString<ApiErrorResponse>(conflict.bodyAsText()).code)
        val foreignDelete = client.delete(path) { asOwner(stranger) }
        assertEquals(HttpStatusCode.NotFound, foreignDelete.status)
        assertEquals("CHARACTER_NOT_FOUND", Json.decodeFromString<ApiErrorResponse>(foreignDelete.bodyAsText()).code)
        repeat(2) {
            val deleted = client.delete(path) { asOwner(owner) }
            assertEquals(HttpStatusCode.NoContent, deleted.status)
            assertEquals("", deleted.bodyAsText())
        }
        assertTrue(client.list().isEmpty())
        assertEquals(HttpStatusCode.Conflict, client.save(id).status)
        val missing = client.delete("/api/v1/characters/char-019a0000-0000-7000-8000-000000000009") { asOwner(owner) }
        assertEquals("CHARACTER_NOT_FOUND", Json.decodeFromString<ApiErrorResponse>(missing.bodyAsText()).code)
    }

    @Test fun retryWithTheSameCreationKeyReturnsTheOriginalId() = testApplication {
        environment { config = databaseConfig(directory) }
        application { module() }
        val key = UUID.randomUUID().toString()
        val created = client.create(requestKey = key)
        assertEquals(HttpStatusCode.Created, created.status)
        val id = Json.decodeFromString<CreatedCharacterResponse>(created.bodyAsText()).id
        val retry = client.create(requestKey = key)
        assertEquals(HttpStatusCode.Created, retry.status)
        assertEquals(id, Json.decodeFromString<CreatedCharacterResponse>(retry.bodyAsText()).id)
        val different = client.create(
            Json.encodeToString(input.copy(name = "Luna")),
            requestKey = key
        )
        assertEquals(HttpStatusCode.Conflict, different.status)
        assertEquals(
            CharactersSaveError.IdempotencyConflict,
            Json.decodeFromString<CharactersSaveError>(different.bodyAsText())
        )
        assertEquals(listOf(id), client.list().map { it.id })
    }

    @Test fun distinguishesStructureFieldValidationAndUnavailableReferences() = testApplication {
        environment { config = databaseConfig(directory) }
        application { module() }
        for (body in listOf("{", "{}", """{"name":"n","personality":"p","appearance":"a","type":"CHILD","id":"ignored"}""")) {
            val response = client.create(body)
            assertEquals(HttpStatusCode.BadRequest, response.status)
            assertEquals("INVALID_REQUEST", Json.decodeFromString<ApiErrorResponse>(response.bodyAsText()).code)
        }
        val invalid = listOf(
            input.copy(name = " ") to ("name" to "REQUIRED"),
            input.copy(name = "a\nb") to ("name" to "SINGLE_LINE"),
            input.copy(name = "🌙".repeat(26)) to ("name" to "TOO_LONG"),
            input.copy(personality = " ") to ("personality" to "REQUIRED"),
            input.copy(personality = "p".repeat(2001)) to ("personality" to "TOO_LONG"),
            input.copy(appearance = " ") to ("appearance" to "REQUIRED"),
            input.copy(appearance = "a".repeat(2001)) to ("appearance" to "TOO_LONG"),
            input.copy(type = "NEW_TYPE") to ("type" to "INVALID_VALUE"),
        )
        for ((request, expected) in invalid) {
            val response = client.create(Json.encodeToString(request))
            assertEquals(HttpStatusCode.UnprocessableEntity, response.status)
            val error = Json.decodeFromString<ApiErrorResponse>(response.bodyAsText())
            assertEquals("validation.${expected.first}.${expected.second.lowercase()}", error.code)
            assertEquals(null, error.field)
            assertEquals(null, error.reason)
            assertEquals(error.code, Json.decodeFromString<CharactersSaveError>(response.bodyAsText()).code)
        }
        val reference = client.create(
            """{"name":"n","personality":"p","appearance":"a","type":"CHILD","avatar":{"id":"missing","url":"https://example.test/avatar"}}"""
        )
        assertEquals(HttpStatusCode.UnprocessableEntity, reference.status)
        assertEquals("REFERENCE_UNAVAILABLE", Json.decodeFromString<ApiErrorResponse>(reference.bodyAsText()).code)
        assertTrue(client.list().isEmpty())
        for (header in listOf(null, "invalid")) {
            val response = client.get("/api/v1/characters") { header?.let { header("X-Owner-Id", it) } }
            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }
        val badId = client.put("/api/v1/characters/not-a-uuid") { asOwner(owner); contentType(ContentType.Application.Json); setBody(Json.encodeToString(input)) }
        assertEquals("INVALID_REQUEST", Json.decodeFromString<ApiErrorResponse>(badId.bodyAsText()).code)
    }
}
