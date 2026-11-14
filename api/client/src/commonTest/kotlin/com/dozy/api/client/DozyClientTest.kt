package com.dozy.api.client

import com.dozy.api.client.place.v1.PlacesListError
import com.dozy.api.client.place.v1.PlacesSaveError
import com.dozy.api.contracts.v1.ApiErrorResponse
import com.dozy.api.contracts.v1.place.SavePlaceRequest
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

class DozyClientTest {
    private val owner = "60c55c02-8d8b-46c2-a0cb-9a2d241eeea0"

    @Test
    fun `lists through v1 with installation identity and tolerates additive response fields`() = runTest {
        val http = HttpClient(MockEngine { request ->
            assertEquals("https://example.test/api/v1/places", request.url.toString())
            assertEquals(owner, request.headers["X-Installation-Id"])
            respond("""{"places":[{"id":"builtin:woods","name":"Woods","description":"Quiet","origin":"SYSTEM","editable":false,"newField":true}],"extra":1}""",
                headers = headersOf(HttpHeaders.ContentType, "application/json"))
        })
        try {
            val places = value(sdk(http).places.v1.list()).places
            assertEquals("SYSTEM", places.single().origin)
            assertEquals(null, places.single().inhabitants)
        } finally { http.close() }
    }

    @Test
    fun `encodes path ID and sends only editable fields for both success statuses`() = runTest {
        var calls = 0
        val input = SavePlaceRequest("Garden", "Quiet", null)
        val http = HttpClient(MockEngine { request ->
            assertEquals("PUT", request.method.value)
            assertEquals("/api/v1/places/custom%2Fone%20two", request.url.encodedPath)
            assertEquals(owner, request.headers["X-Installation-Id"])
            val body = (request.body as TextContent).text
            assertEquals(setOf("name", "description", "inhabitants"), Json.parseToJsonElement(body).jsonObject.keys)
            assertEquals(input, Json.decodeFromString<SavePlaceRequest>(body))
            respond("", if (calls++ == 0) HttpStatusCode.Created else HttpStatusCode.NoContent)
        })
        try {
            repeat(2) { assertEquals(ApiResult.Success(Unit), sdk(http).places.v1.save("custom/one two", input)) }
            assertEquals(2, calls)
        } finally { http.close() }
    }

    @Test
    fun `maps defined errors by both status and code`() = runTest {
        val errors = listOf(
            Triple(403, """{"code":"FORBIDDEN"}""", PlacesSaveError.Forbidden),
            Triple(409, """{"code":"CONFLICT"}""", PlacesSaveError.Conflict),
            Triple(503, """{"code":"UNAVAILABLE"}""", PlacesSaveError.Unavailable),
            Triple(422, """{"code":"INVALID_REQUEST","field":"name","reason":"REQUIRED"}""",
                PlacesSaveError.Validation(PlacesSaveError.Validation.Field.NAME, PlacesSaveError.Validation.Reason.REQUIRED)),
            Triple(422, """{"code":"INVALID_REQUEST","field":"description","reason":"TOO_LONG"}""",
                PlacesSaveError.Validation(PlacesSaveError.Validation.Field.DESCRIPTION, PlacesSaveError.Validation.Reason.TOO_LONG)),
            Triple(422, """{"code":"INVALID_REQUEST","field":"inhabitants","reason":"TOO_LONG"}""",
                PlacesSaveError.Validation(PlacesSaveError.Validation.Field.INHABITANTS, PlacesSaveError.Validation.Reason.TOO_LONG)),
        )
        for ((status, body, expected) in errors) {
            val http = HttpClient(MockEngine { respond(body, HttpStatusCode.fromValue(status)) }) { expectSuccess = true }
            try { assertEquals(ApiResult.Failure(expected), sdk(http).places.v1.save("one", SavePlaceRequest("Garden", "Quiet"))) }
            finally { http.close() }
        }
        val unavailable = HttpClient(MockEngine { respond("""{"code":"UNAVAILABLE"}""", HttpStatusCode.ServiceUnavailable) })
        try { assertEquals(ApiResult.Failure(PlacesListError.Unavailable), sdk(unavailable).places.v1.list()) }
        finally { unavailable.close() }
    }

    @Test
    fun `preserves new codes fields reasons and unexpected status combinations as Unknown`() = runTest {
        val replies = listOf(
            409 to """{"code":"NEW_RULE"}""",
            403 to """{"code":"FORBID"}""",
            400 to """{"code":"NEW_RULE"}""",
            500 to """{"code":"CONFLICT"}""",
            422 to """{"code":"INVALID_REQUEST","field":"newField","reason":"REQUIRED"}""",
            422 to """{"code":"INVALID_REQUEST","field":"name","reason":"NEW_RULE"}""",
            422 to """{"code":"INVALID_REQUEST","field":"inhabitants","reason":"REQUIRED"}""",
        )
        for ((status, body) in replies) {
            val http = HttpClient(MockEngine { respond(body, HttpStatusCode.fromValue(status)) })
            try {
                val error = assertIs<PlacesSaveError.Unknown>(failure(sdk(http).places.v1.save("one", SavePlaceRequest("Garden", "Quiet"))))
                assertEquals(status, error.status)
                assertEquals(Json.decodeFromString<ApiErrorResponse>(body), error.error)
                assertIs<PlacesListError.Unknown>(failure(sdk(http).places.v1.list()))
            } finally { http.close() }
        }
    }

    @Test
    fun `invalid request and installation errors throw outside ApiResult`() = runTest {
        for (code in listOf("INVALID_REQUEST", "INVALID_INSTALLATION_ID")) {
            val http = HttpClient(MockEngine { respond("""{"code":"$code"}""", HttpStatusCode.BadRequest) })
            try {
                val list = assertFailsWith<InvalidRequestException> { sdk(http).places.v1.list() }
                assertEquals(code, list.error.code)
                assertEquals(400, list.status)
                assertFailsWith<InvalidRequestException> { sdk(http).places.v1.save("one", SavePlaceRequest("Garden", "Quiet")) }
            } finally { http.close() }
        }
    }

    @Test
    fun `malformed error payloads and unexpected success responses throw outside ApiResult`() = runTest {
        for (body in listOf("not JSON", "{}")) {
            val http = HttpClient(MockEngine { respond(body, HttpStatusCode.ServiceUnavailable) })
            try {
                assertFailsWith<SerializationException> { sdk(http).places.v1.list() }
                assertFailsWith<SerializationException> { sdk(http).places.v1.save("one", SavePlaceRequest("Garden", "Quiet")) }
            } finally { http.close() }
        }
        val malformedValidation = HttpClient(MockEngine { respond("""{"code":"INVALID_REQUEST"}""", HttpStatusCode.UnprocessableEntity) })
        val unexpectedSuccess = HttpClient(MockEngine { respond("", HttpStatusCode.OK) })
        try {
            assertFailsWith<InvalidResponseException> { sdk(malformedValidation).places.v1.save("one", SavePlaceRequest("Garden", "Quiet")) }
            assertFailsWith<InvalidResponseException> { sdk(unexpectedSuccess).places.v1.save("one", SavePlaceRequest("Garden", "Quiet")) }
        } finally { malformedValidation.close(); unexpectedSuccess.close() }
    }

    @Test
    fun `transport failures remain exceptions for reads and writes`() = runTest {
        val transport = IOException("Transport failed")
        val http = HttpClient(MockEngine { throw transport })
        try {
            assertEquals(transport.message, assertFailsWith<IOException> { sdk(http).places.v1.list() }.message)
            assertEquals(transport.message, assertFailsWith<IOException> { sdk(http).places.v1.save("one", SavePlaceRequest("Garden", "Quiet")) }.message)
        } finally { http.close() }
    }

    @Test
    fun `disabled configuration and blank ID never send requests`() = runTest {
        val http = HttpClient(MockEngine { error("Must not send a request") })
        try {
            val disabled = DozyClient(http, DozyClientConfig(null), InstallationIdProvider { owner })
            assertFailsWith<ApiDisabledException> { disabled.places.v1.list() }
            assertFailsWith<ApiDisabledException> { disabled.places.v1.save("one", SavePlaceRequest("Garden", "Quiet")) }
            assertFailsWith<IllegalArgumentException> { sdk(http).places.v1.save(" ", SavePlaceRequest("Garden", "Quiet")) }
        } finally { http.close() }
    }

    @Test
    fun `does not swallow cancellation or malformed successful responses`() = runTest {
        val cancelled = HttpClient(MockEngine { throw CancellationException("Cancelled") })
        val malformed = HttpClient(MockEngine { respond("""{"places":[{}]}""") })
        try {
            assertFailsWith<CancellationException> { sdk(cancelled).places.v1.list() }
            assertFailsWith<CancellationException> { sdk(cancelled).places.v1.save("one", SavePlaceRequest("Garden", "Quiet")) }
            assertFailsWith<SerializationException> { sdk(malformed).places.v1.list() }
        } finally { cancelled.close(); malformed.close() }
    }

    @Test
    fun `different installations can share one caller owned HTTP client`() = runTest {
        val ids = mutableListOf<String?>()
        val http = HttpClient(MockEngine {
            ids += it.headers["X-Installation-Id"]
            respond("""{"places":[]}""")
        })
        try {
            sdk(http).places.v1.list()
            DozyClient(http, DozyClientConfig("https://example.test"), InstallationIdProvider { "another-installation" }).places.v1.list()
            assertEquals<List<String?>>(listOf(owner, "another-installation"), ids)
        } finally { http.close() }
    }

    private fun <T, E> value(result: ApiResult<T, E>): T = assertIs<ApiResult.Success<T>>(result).value
    private fun <T, E> failure(result: ApiResult<T, E>): E = assertIs<ApiResult.Failure<E>>(result).error

    private fun sdk(http: HttpClient) = DozyClient(http, DozyClientConfig("https://example.test/"), InstallationIdProvider { owner })
}
