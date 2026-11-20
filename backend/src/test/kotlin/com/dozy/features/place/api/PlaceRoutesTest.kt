package com.dozy.features.place.api

import com.dozy.api.contracts.v1.ApiErrorResponse
import com.dozy.api.contracts.v1.place.CreatedPlaceResponse
import com.dozy.api.contracts.v1.place.PlaceResponse
import com.dozy.api.contracts.v1.place.PlacesResponse
import com.dozy.api.contracts.v1.place.PlacesSaveError
import com.dozy.api.contracts.v1.place.SavePlaceRequest
import com.dozy.asOwner
import com.dozy.databaseConfig
import com.dozy.domain.id.AvatarJobId
import com.dozy.domain.id.CharacterId
import com.dozy.domain.id.IdempotencyKey
import com.dozy.domain.id.OwnerId
import com.dozy.domain.id.PlaceId
import com.dozy.features.asset.domain.repository.AssetRepository
import com.dozy.features.character.domain.model.Character
import com.dozy.features.character.domain.model.SaveCharacter
import com.dozy.features.character.domain.model.SaveCharacterResult
import com.dozy.features.character.domain.repository.CharacterRepository
import com.dozy.features.characteravatar.domain.model.AvatarInput
import com.dozy.features.characteravatar.domain.model.AvatarJob
import com.dozy.features.characteravatar.domain.repository.AvatarJobs
import com.dozy.features.place.domain.model.Place
import com.dozy.features.place.domain.model.SavePlace
import com.dozy.features.place.domain.model.SavePlaceResult
import com.dozy.features.place.domain.repository.PlaceRepository
import com.dozy.features.story.UnusedGenerationRepository
import com.dozy.features.story.domain.repository.GenerationRepository
import com.dozy.module
import io.ktor.client.HttpClient
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsBytes
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.plugins.di.dependencies
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.jupiter.api.io.TempDir
import java.io.ByteArrayInputStream
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.sql.SQLException
import java.util.UUID
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class PlaceRoutesTest {
    @TempDir lateinit var directory: Path
    private val owner = requireNotNull(OwnerId.parse("owner-019a0000-0000-7000-8000-000000000001"))
    private val stranger = requireNotNull(OwnerId.parse("owner-019a0000-0000-7000-8000-000000000002"))
    private val collection = "/api/v1/places"

    @Test
    fun `lists builtins then saves and replaces a normalized custom place`() = testApplication {
        configurePlacesTest()
        val initial = client.places()
        assertEquals(3, initial.size)
        assertEquals(setOf("builtin:village", "builtin:island", "builtin:woods"), initial.map { it.id }.toSet())
        initial.forEach { assertFalse(it.editable); assertEquals("SYSTEM", it.origin) }
        assertError(client.save("place-019a0000-0000-7000-8000-000000000003"), HttpStatusCode.NotFound, "place.not_found")
        assertEquals(3, client.places().size)
        val created = client.create(body = body(name = "  Garden  ", inhabitants = "Owls"))
        assertEquals(HttpStatusCode.Created, created.status)
        val creation = Json.decodeFromString<CreatedPlaceResponse>(created.bodyAsText())
        val id = creation.id
        assertEquals(7, UUID.fromString(id.removePrefix("place-")).version())
        val saved = client.places().first()
        assertEquals(id, saved.id)
        assertEquals("Garden", saved.name)
        assertEquals("USER", saved.origin)
        assertEquals(true, saved.editable)
        assertEquals("Owls", saved.inhabitants)
        assertEquals(creation.imageUrl, saved.imageUrl)
        assertNotNull(creation.imageUrl)
        val updated = client.save(id)
        assertEquals(HttpStatusCode.NoContent, updated.status)
        assertEquals("", updated.bodyAsText())
        val owned = client.places()
        assertEquals(4, owned.size)
        assertNull(owned.first().inhabitants)
        assertEquals(HttpStatusCode.NoContent, client.save(id).status)
        assertEquals(4, client.places().size)
    }

    @Test
    fun `listed place thumbnails are PNGs served behind bearer authentication`() = testApplication {
        configurePlacesTest()
        val thumbnails = client.places().map { URI(assertNotNull(it.imageUrl)).path }.toSet()
        assertEquals(setOf("/api/v1/place-icons/forest.png", "/api/v1/place-icons/cottage.png"), thumbnails)
        for (path in thumbnails) {
            assertEquals(HttpStatusCode.Unauthorized, client.get(path).status)
            val response = client.get(path) { asOwner(owner) }
            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals("image/png", response.headers["Content-Type"]?.substringBefore(';'))
            val image = assertNotNull(ImageIO.read(ByteArrayInputStream(response.bodyAsBytes())))
            assertEquals(256, image.width)
            assertEquals(256, image.height)
        }
    }

    @Test
    fun `replays creation per owner and rejects reusing the key for different input`() = testApplication {
        configurePlacesTest()
        val key = UUID.randomUUID().toString()
        val first = client.create(requestKey = key)
        assertEquals(HttpStatusCode.Created, first.status)
        val id = Json.decodeFromString<CreatedPlaceResponse>(first.bodyAsText()).id
        val retry = client.create(body = body(name = " Garden "), requestKey = key)
        assertEquals(HttpStatusCode.Created, retry.status)
        assertEquals(id, Json.decodeFromString<CreatedPlaceResponse>(retry.bodyAsText()).id)
        assertError(client.create(body = body(name = "Different"), requestKey = key), HttpStatusCode.Conflict, "IDEMPOTENCY_CONFLICT")
        assertError(client.create(requestKey = "not-a-uuid"), HttpStatusCode.BadRequest, "INVALID_REQUEST")
        val another = client.create(ownerId = stranger, requestKey = key)
        assertEquals(HttpStatusCode.Created, another.status)
        assertEquals(listOf(id), client.places().filter { it.origin == "USER" }.map { it.id })
        assertFalse(client.places(stranger).any { it.id == id })
    }

    @Test
    fun `reads and writes require valid bearer credentials`() = testApplication {
        configurePlacesTest()
        assertEquals(HttpStatusCode.Unauthorized, client.get("/api/v1/places") {
            header("X-Installation-Id", owner.value)
        }.status)
        assertEquals(HttpStatusCode.Unauthorized, client.get("/api/v1/places") {
            bearerAuth("invalid")
        }.status)
        assertEquals(HttpStatusCode.Unauthorized, client.create(ownerId = null).status)
    }

    @Test
    fun `accepts canonical UUID case and retains origin isolation`() = testApplication {
        configurePlacesTest()
        val uppercase = "owner-019A0000-0000-7000-8000-00000000000A"
        val ownerId = requireNotNull(OwnerId.parse(uppercase))
        val created = client.create(ownerId = ownerId)
        assertEquals(HttpStatusCode.Created, created.status)
        val id = Json.decodeFromString<CreatedPlaceResponse>(created.bodyAsText()).id
        assertEquals(4, client.places(requireNotNull(OwnerId.parse(uppercase.lowercase()))).size)
        assertEquals(3, client.places(stranger).size)
        assertError(client.save(id, ownerId = stranger), HttpStatusCode.Conflict, "place.conflict")
        assertEquals(4, client.places(ownerId).size)
    }

    @Test
    fun `protects predefined namespace even for unknown IDs`() = testApplication {
        configurePlacesTest()
        for (builtin in listOf("builtin:woods", "builtin:missing")) {
            assertError(client.save(path = builtin, body = body()), HttpStatusCode.Forbidden, "place.not_editable")
        }
        assertEquals("Whispering woods", client.places().single { it.id == "builtin:woods" }.name)
    }

    @Test
    fun `rejects malformed JSON and missing fields`() = testApplication {
        configurePlacesTest()
        val invalidBodies = listOf(
            "abc", "{", "{}", """{"name":"Garden"}""",
            """{"name":"Garden","description":"Quiet","origin":"SYSTEM","editable":true}""",
            """{"id":"provided","name":"Garden","description":"Quiet"}""",
        )
        for (invalid in invalidBodies) {
            assertError(client.create(body = invalid), HttpStatusCode.BadRequest, "INVALID_REQUEST")
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
            val response = client.create(body = invalid)
            assertFieldError(response, expected.first, expected.second)
        }
        assertEquals(3, client.places().size)
    }

    @Test
    fun `DI fake overrides production repository and database failures have safe errors`() = testApplication {
        environment { config = databaseConfig(directory) }
        application {
            dependencies.provide<AssetRepository> { AssetRepository { _, _ -> null } }
            dependencies.provide<GenerationRepository> { UnusedGenerationRepository() }
            dependencies.provide<AvatarJobs> { object : AvatarJobs {
                override suspend fun recover() = Unit
                override suspend fun claim(): AvatarJob? = null
                override suspend fun create(ownerId: OwnerId, input: AvatarInput): AvatarJob = error("Unexpected call")
                override suspend fun find(ownerId: OwnerId, id: AvatarJobId): AvatarJob? = error("Unexpected call")
                override suspend fun complete(job: AvatarJob, success: Boolean) = error("Unexpected call")
            } }
            dependencies.provide<PlaceRepository> { FailingRepository() }
            dependencies.provide<CharacterRepository> { FailingCharacterRepository() }
            module()
        }
        assertError(client.get("/api/v1/places") { asOwner(owner) },
            HttpStatusCode.ServiceUnavailable, "UNAVAILABLE")
        assertError(client.create(), HttpStatusCode.ServiceUnavailable, "UNAVAILABLE")
        assertFalse(Files.exists(directory.resolve("dozy.db")))
    }

    private fun ApplicationTestBuilder.configurePlacesTest() {
        environment { config = databaseConfig(directory) }
        application { module() }
    }

    private suspend fun HttpClient.places(ownerId: OwnerId = owner): List<PlaceResponse> {
        val response = get("/api/v1/places") { asOwner(ownerId) }
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("application/json", response.headers["Content-Type"]?.substringBefore(';'))
        return Json.decodeFromString<PlacesResponse>(response.bodyAsText()).places
    }

    private suspend fun HttpClient.create(
        ownerId: OwnerId? = owner,
        body: String = body(),
        requestKey: String? = null,
    ) = post(collection) {
        ownerId?.let { asOwner(it) }
        contentType(ContentType.Application.Json)
        requestKey?.let { header("Idempotency-Key", it) }
        setBody(body)
    }

    private suspend fun HttpClient.save(
        path: String,
        ownerId: OwnerId? = owner,
        body: String = body(),
    ) = put("$collection/$path") {
        ownerId?.let { asOwner(it) }
        contentType(ContentType.Application.Json)
        setBody(body)
    }

    private fun body(name: String = "Garden", description: String = "Quiet garden", inhabitants: String? = null) =
        Json.encodeToString(SavePlaceRequest(name, description, inhabitants))

    private suspend fun assertError(response: HttpResponse, status: HttpStatusCode, code: String) {
        assertEquals(status, response.status)
        val error = decodeError(response)
        assertEquals(code, error.code)
        if (status == HttpStatusCode.Forbidden || status == HttpStatusCode.Conflict) {
            assertEquals(PlacesSaveError.fromCode(code), Json.decodeFromString<PlacesSaveError>(response.bodyAsText()))
        }
        assertEquals(null, error.field)
        assertEquals(null, error.reason)
    }

    private suspend fun assertFieldError(response: HttpResponse, field: String, reason: String) {
        assertEquals(HttpStatusCode.UnprocessableEntity, response.status)
        val error = decodeError(response)
        assertEquals("validation.$field.${reason.lowercase()}", error.code)
        assertEquals(PlacesSaveError.fromCode(error.code), Json.decodeFromString<PlacesSaveError>(response.bodyAsText()))
        assertNull(error.field)
        assertNull(error.reason)
    }

    private suspend fun decodeError(response: HttpResponse): ApiErrorResponse {
        val body = response.bodyAsText()
        assertEquals(setOf("code", "field", "reason"), Json.parseToJsonElement(body).jsonObject.keys)
        return Json.decodeFromString(body)
    }

    private class FailingCharacterRepository : CharacterRepository {
        override suspend fun list(ownerId: OwnerId): List<Character> = throw SQLException("Private database path")
        override suspend fun create(ownerId: OwnerId, command: SaveCharacter, requestKey: IdempotencyKey?): SaveCharacterResult =
            throw SQLException("Private database path")

        override suspend fun save(ownerId: OwnerId, id: CharacterId, command: SaveCharacter): SaveCharacterResult =
            throw SQLException("Private database path")
        override suspend fun delete(ownerId: OwnerId, id: CharacterId): Boolean = throw SQLException("Private database path")
    }

    private class FailingRepository : PlaceRepository {
        override suspend fun list(ownerId: OwnerId): List<Place> = throw SQLException("Private database path")
        override suspend fun create(ownerId: OwnerId, command: SavePlace, requestKey: IdempotencyKey?): SavePlaceResult =
            throw SQLException("Private database path")

        override suspend fun save(ownerId: OwnerId, id: PlaceId, command: SavePlace): SavePlaceResult =
            throw SQLException("Private database path")
    }
}
