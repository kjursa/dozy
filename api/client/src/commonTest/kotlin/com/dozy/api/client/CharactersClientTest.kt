package com.dozy.api.client

import com.dozy.api.client.character.v1.CharactersDeleteError
import com.dozy.api.client.character.v1.CharactersListError
import com.dozy.api.client.character.v1.CharactersSaveError
import com.dozy.api.contracts.v1.ValidationReason
import com.dozy.api.contracts.v1.character.CharacterField
import com.dozy.api.contracts.v1.character.SaveCharacterRequest
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class CharactersClientTest {
    private val input = SaveCharacterRequest("Oliver", "Curious", "Green eyes", "CHILD")
    private fun sdk(http: HttpClient) = DozyClient(http, DozyClientConfig("https://example.test/"), InstallationIdProvider { "installation" }).characters.v1

    @Test fun bareArrayAndFullReplacementWireContract() = runTest {
        var writes = 0
        val http = HttpClient(MockEngine { request ->
            assertEquals("installation", request.headers["X-Installation-Id"])
            if (request.method.value == "GET") {
                assertEquals("/api/v1/characters", request.url.encodedPath)
                respond("""[{"id":"one","name":"Oliver","personality":"","appearance":"","type":"CHILD","extra":1}]""")
            } else {
                assertEquals("PUT", request.method.value)
                assertEquals("/api/v1/characters/one%2Ftwo%20three", request.url.encodedPath)
                val body = (request.body as TextContent).text
                assertEquals(setOf("name", "personality", "appearance", "type", "avatar"), Json.parseToJsonElement(body).jsonObject.keys)
                assertEquals(input, Json.decodeFromString<SaveCharacterRequest>(body))
                respond("", if (writes++ == 0) HttpStatusCode.Created else HttpStatusCode.NoContent)
            }
        })
        try {
            assertEquals("Oliver", (sdk(http).list() as ApiResult.Success).value.single().name)
            repeat(2) { assertEquals(ApiResult.Success(Unit), sdk(http).save("one/two three", input)) }
        } finally { http.close() }
    }

    @Test fun mapsKnownErrorsByCodeAndPreservesUnknownDetails() = runTest {
        val cases = listOf(
            """{"code":"CONFLICT"}""" to CharactersSaveError.Conflict,
            """{"code":"REFERENCE_UNAVAILABLE"}""" to CharactersSaveError.ReferenceUnavailable,
            """{"code":"UNAVAILABLE"}""" to CharactersSaveError.Unavailable,
            """{"code":"VALIDATION_FAILED","field":"name","reason":"SINGLE_LINE"}""" to CharactersSaveError.Validation(CharacterField.NAME, ValidationReason.SINGLE_LINE),
            """{"code":"VALIDATION_FAILED","field":"type","reason":"INVALID_VALUE"}""" to CharactersSaveError.Validation(CharacterField.TYPE, ValidationReason.INVALID_VALUE),
        )
        for ((body, expected) in cases) {
            val http = HttpClient(MockEngine { respond(body, HttpStatusCode.InternalServerError) }) { expectSuccess = true }
            try { assertEquals(ApiResult.Failure(expected), sdk(http).save("one", input)) } finally { http.close() }
        }
        for (body in listOf(
            """{"code":"NEW_CODE"}""",
            """{"code":"VALIDATION_FAILED","field":"new","reason":"REQUIRED"}""",
            """{"code":"VALIDATION_FAILED","field":"name","reason":"NEW"}""",
            """{"code":"VALIDATION_FAILED","field":"type","reason":"REQUIRED"}""",
        )) {
            val http = HttpClient(MockEngine { respond(body, HttpStatusCode.Conflict) })
            try { assertIs<CharactersSaveError.Unknown>((sdk(http).save("one", input) as ApiResult.Failure).error) } finally { http.close() }
        }
    }

    @Test fun onlyKnownNotFoundCodeAcknowledgesDeletionAndAllRequestsHaveIdentity() = runTest {
        var calls = 0
        val http = HttpClient(MockEngine { request ->
            assertEquals("DELETE", request.method.value)
            assertEquals("installation", request.headers["X-Installation-Id"])
            assertEquals("/api/v1/characters/one%2Ftwo", request.url.encodedPath)
            when (calls++) {
                0 -> respond("", HttpStatusCode.NoContent)
                1 -> respond("""{"code":"CHARACTER_NOT_FOUND"}""", HttpStatusCode.InternalServerError)
                2 -> respond("""{"code":"UNKNOWN"}""", HttpStatusCode.NotFound)
                else -> respond("""{"code":"UNAVAILABLE"}""", HttpStatusCode.NotFound)
            }
        })
        try {
            repeat(2) { assertEquals(ApiResult.Success(Unit), sdk(http).delete("one/two")) }
            assertIs<CharactersDeleteError.Unknown>((sdk(http).delete("one/two") as ApiResult.Failure).error)
            assertEquals(ApiResult.Failure(CharactersDeleteError.Unavailable), sdk(http).delete("one/two"))
        } finally { http.close() }
    }

    @Test fun transportFailuresPropagateForEveryOperation() = runTest {
        val http = HttpClient(MockEngine { throw IOException("offline") })
        try {
            assertFailsWith<IOException> { sdk(http).list() }
            assertFailsWith<IOException> { sdk(http).save("one", input) }
            assertFailsWith<IOException> { sdk(http).delete("one") }
        } finally { http.close() }
    }

    @Test fun structuralFailuresCancellationAndDisabledApiRemainExceptions() = runTest {
        for (code in listOf("INVALID_REQUEST", "INVALID_INSTALLATION_ID")) {
            val http = HttpClient(MockEngine { respond("""{"code":"$code"}""", HttpStatusCode.Conflict) })
            try {
                assertFailsWith<InvalidRequestException> { sdk(http).list() }
                assertFailsWith<InvalidRequestException> { sdk(http).save("one", input) }
                assertFailsWith<InvalidRequestException> { sdk(http).delete("one") }
            } finally { http.close() }
        }
        val malformed = HttpClient(MockEngine { respond("{}") })
        val cancelled = HttpClient(MockEngine { throw CancellationException() })
        val disabled = HttpClient(MockEngine { error("No request allowed") })
        try {
            assertFailsWith<SerializationException> { sdk(malformed).list() }
            assertFailsWith<InvalidResponseException> { sdk(malformed).save("one", input) }
            assertFailsWith<InvalidResponseException> { sdk(malformed).delete("one") }
            assertFailsWith<CancellationException> { sdk(cancelled).list() }
            assertFailsWith<CancellationException> { sdk(cancelled).save("one", input) }
            assertFailsWith<CancellationException> { sdk(cancelled).delete("one") }
            val client = DozyClient(disabled, DozyClientConfig(null), InstallationIdProvider { "id" }).characters.v1
            assertFailsWith<ApiDisabledException> { client.list() }
            assertFailsWith<ApiDisabledException> { client.save("one", input) }
            assertFailsWith<ApiDisabledException> { client.delete("one") }
        } finally { malformed.close(); cancelled.close(); disabled.close() }
    }
}
