package com.dozy.api.client

import com.dozy.api.client.place.v1.PlacesListError
import com.dozy.api.client.place.v1.PlacesListResult
import com.dozy.api.client.place.v1.PlacesSaveResult
import com.dozy.api.contracts.v1.place.PlacesResponse
import com.dozy.api.client.place.v1.PlacesSaveError
import com.dozy.api.contracts.v1.place.SavePlaceRequest
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.HttpRequestTimeoutException
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
            repeat(2) { assertEquals(PlacesSaveResult.Success, sdk(http).places.v1.save("custom/one two", input)) }
            assertEquals(2, calls)
        } finally { http.close() }
    }

    @Test
    fun `maps every flat contract code without requiring field or reason`() = runTest {
        val errors = listOf(
            "place.not_editable" to PlacesSaveError.Forbidden,
            "place.conflict" to PlacesSaveError.Conflict,
            "validation.name.required" to PlacesSaveError.NameRequired,
            "validation.name.too_long" to PlacesSaveError.NameTooLong,
            "validation.description.required" to PlacesSaveError.DescriptionRequired,
            "validation.description.too_long" to PlacesSaveError.DescriptionTooLong,
            "validation.inhabitants.too_long" to PlacesSaveError.InhabitantsTooLong,
        )
        for ((code, expected) in errors) {
            val http = HttpClient(MockEngine { respond("""{"code":"$code"}""", HttpStatusCode.UnprocessableEntity) }) { expectSuccess = true }
            try { assertEquals(PlacesSaveResult.ApiError(expected), sdk(http).places.v1.save("one", SavePlaceRequest("Garden", "Quiet"))) }
            finally { http.close() }
        }
    }

    @Test
    fun `unknown API codes preserve envelope and status`() = runTest {
        val http = HttpClient(MockEngine { respond("""{"code":"validation.new_field.new_rule"}""", HttpStatusCode.UnprocessableEntity) })
        try {
            val result = assertIs<PlacesSaveResult.ApiError>(sdk(http).places.v1.save("one", SavePlaceRequest("Garden", "Quiet")))
            val error = assertIs<PlacesSaveError.Unknown>(result.error)
            assertEquals(422, error.status)
            assertEquals("validation.new_field.new_rule", error.error.code)
            assertIs<PlacesListError.Unknown>(assertIs<PlacesListResult.ApiError>(sdk(http).places.v1.list()).error)
        } finally { http.close() }
    }

    @Test
    fun `server status wins over known codes HTML and missing bodies`() = runTest {
        for (status in listOf(500, 503, 504)) {
            for (body in listOf("<html>Unavailable</html>", "", """{"code":"place.conflict"}""")) {
                val http = HttpClient(MockEngine { respond(body, HttpStatusCode.fromValue(status)) }) { expectSuccess = true }
                try {
                    assertEquals(TechnicalError.Server(status), failure(sdk(http).places.v1.list()))
                    assertEquals(TechnicalError.Server(status), failure(sdk(http).places.v1.save("one", SavePlaceRequest("Garden", "Quiet"))))
                } finally { http.close() }
            }
        }
    }

    @Test
    fun `invalid requests malformed payloads and unexpected statuses are communication errors`() = runTest {
        for ((status, body) in listOf(
            400 to """{"code":"INVALID_REQUEST"}""",
            400 to """{"code":"INVALID_INSTALLATION_ID"}""",
            400 to "<html>Bad request</html>",
            422 to "{}",
            200 to "<html>Not JSON</html>",
            200 to """{"places":[{}]}""",
            302 to "",
        )) {
            val http = HttpClient(MockEngine { respond(body, HttpStatusCode.fromValue(status)) }) { followRedirects = false }
            try {
                assertIs<TechnicalError.Communication>(failure(sdk(http).places.v1.list()))
                assertIs<TechnicalError.Communication>(failure(sdk(http).places.v1.save("one", SavePlaceRequest("Garden", "Quiet"))))
            } finally { http.close() }
        }
    }

    @Test
    fun `transport failures return their cause for both operations`() = runTest {
        val transport = IOException("Transport failed")
        val http = HttpClient(MockEngine { throw transport })
        try {
            assertEquals(transport.message, assertIs<TechnicalError.Communication>(failure(sdk(http).places.v1.list())).exception.message)
            assertIs<TechnicalError.Communication>(failure(sdk(http).places.v1.save("one", SavePlaceRequest("Garden", "Quiet"))))
        } finally { http.close() }
    }

    @Test
    fun `request timeout is returned as a communication failure`() = runTest {
        val http = HttpClient(MockEngine { request -> throw HttpRequestTimeoutException(request) })
        try {
            val error = assertIs<TechnicalError.Communication>(failure(sdk(http).places.v1.list()))
            assertIs<HttpRequestTimeoutException>(error.exception)
            assertIs<TechnicalError.Communication>(failure(sdk(http).places.v1.save("one", SavePlaceRequest("Garden", "Quiet"))))
        } finally { http.close() }
    }

    @Test
    fun `all Exceptions including local misuse become results while cancellation propagates`() = runTest {
        val http = HttpClient(MockEngine { error("Must not send a request") })
        val cancelled = HttpClient(MockEngine { throw CancellationException("Cancelled") })
        val programmingBug = HttpClient(MockEngine { throw IllegalStateException("Bug") })
        try {
            val disabled = DozyClient(http, DozyClientConfig(null), InstallationIdProvider { owner })
            assertIs<ApiDisabledException>(assertIs<TechnicalError.Communication>(failure(disabled.places.v1.list())).exception)
            assertIs<TechnicalError.Communication>(failure(disabled.places.v1.save("one", SavePlaceRequest("Garden", "Quiet"))))
            assertIs<IllegalArgumentException>(assertIs<TechnicalError.Communication>(failure(sdk(http).places.v1.save(" ", SavePlaceRequest("Garden", "Quiet")))).exception)
            assertFailsWith<CancellationException> { sdk(cancelled).places.v1.list() }
            assertFailsWith<CancellationException> { sdk(cancelled).places.v1.save("one", SavePlaceRequest("Garden", "Quiet")) }
            assertIs<IllegalStateException>(assertIs<TechnicalError.Communication>(failure(sdk(programmingBug).places.v1.list())).exception)
            assertIs<IllegalStateException>(assertIs<TechnicalError.Communication>(failure(sdk(programmingBug).places.v1.save("one", SavePlaceRequest("Garden", "Quiet")))).exception)
        } finally { http.close(); cancelled.close(); programmingBug.close() }
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

    private fun value(result: PlacesListResult): PlacesResponse = assertIs<PlacesListResult.Success>(result).response

    private fun failure(result: PlacesListResult): Any = when (result) {
        is PlacesListResult.ApiError -> result.error
        is PlacesListResult.TechnicalError -> result.error
        is PlacesListResult.Success -> error("Expected failure")
    }

    private fun failure(result: PlacesSaveResult): Any = when (result) {
        is PlacesSaveResult.ApiError -> result.error
        is PlacesSaveResult.TechnicalError -> result.error
        PlacesSaveResult.Success -> error("Expected failure")
    }

    private fun sdk(http: HttpClient) = DozyClient(http, DozyClientConfig("https://example.test/"), InstallationIdProvider { owner })
}
