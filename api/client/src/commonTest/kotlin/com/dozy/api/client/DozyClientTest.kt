package com.dozy.api.client

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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
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
            val places = sdk(http).places.list()
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
            repeat(2) { sdk(http).places.save("custom/one two", input) }
            assertEquals(2, calls)
        } finally { http.close() }
    }

    @Test
    fun `retains structured errors and status without requiring a JSON error body`() = runTest {
        var calls = 0
        val http = HttpClient(MockEngine {
            if (calls++ == 0) respond("""{"code":"INVALID_REQUEST","field":"name","reason":"REQUIRED"}""", HttpStatusCode.UnprocessableEntity)
            else respond("upstream unavailable", HttpStatusCode.ServiceUnavailable)
        }) { expectSuccess = true }
        try {
            val invalid = assertFailsWith<DozyApiException> { sdk(http).places.save("one", SavePlaceRequest("", "Quiet")) }
            assertEquals(422, invalid.status)
            assertEquals("name", invalid.error?.field)
            assertEquals("REQUIRED", invalid.error?.reason)
            val unavailable = assertFailsWith<DozyApiException> { sdk(http).places.list() }
            assertEquals(503, unavailable.status)
            assertEquals(null, unavailable.error)
        } finally { http.close() }
    }

    @Test
    fun `disabled configuration and blank ID never send requests`() = runTest {
        val http = HttpClient(MockEngine { error("Must not send a request") })
        try {
            val disabled = DozyClient(http, DozyClientConfig(null), InstallationIdProvider { owner })
            assertFailsWith<ApiDisabledException> { disabled.places.list() }
            assertFailsWith<ApiDisabledException> { disabled.places.save("one", SavePlaceRequest("Garden", "Quiet")) }
            assertFailsWith<IllegalArgumentException> { sdk(http).places.save(" ", SavePlaceRequest("Garden", "Quiet")) }
        } finally { http.close() }
    }

    @Test
    fun `does not swallow cancellation or malformed successful responses`() = runTest {
        val cancelled = HttpClient(MockEngine { throw CancellationException("Cancelled") })
        val malformed = HttpClient(MockEngine { respond("""{"places":[{}]}""") })
        try {
            assertFailsWith<CancellationException> { sdk(cancelled).places.list() }
            assertFailsWith<SerializationException> { sdk(malformed).places.list() }
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
            sdk(http).places.list()
            DozyClient(http, DozyClientConfig("https://example.test"), InstallationIdProvider { "another-installation" }).places.list()
            assertEquals<List<String?>>(listOf(owner, "another-installation"), ids)
        } finally { http.close() }
    }

    private fun sdk(http: HttpClient) = DozyClient(http, DozyClientConfig("https://example.test/"), InstallationIdProvider { owner })
}
