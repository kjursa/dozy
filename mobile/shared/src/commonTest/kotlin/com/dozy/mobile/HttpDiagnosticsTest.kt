package com.dozy.mobile

import com.dozy.api.client.DozyClient
import com.dozy.api.client.DozyClientConfig
import com.dozy.api.client.auth.InMemoryAuthSessionStorage
import com.dozy.api.client.auth.StoredAuthSession
import com.dozy.mobile.core.data.api.HttpEngineFactory
import com.dozy.mobile.core.data.api.createHttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.encodedPath
import io.ktor.http.headersOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.test.runTest

class HttpDiagnosticsTest {
    @Test
    fun logsUnauthorizedRefreshAndRetryWithoutCredentials() = runTest {
        val messages = Channel<String>(Channel.UNLIMITED)
        lateinit var lines: List<String>
        val authTrace = mutableListOf<String>()
        val storage = InMemoryAuthSessionStorage()
        storage.save(StoredAuthSession("owner", "old-access", "old-refresh"))
        val json = headersOf(HttpHeaders.ContentType, "application/json")
        val requestIds = mutableListOf<String>()
        val engine = MockEngine { request ->
            requestIds.add(assertNotNull(request.headers[HttpHeaders.XRequestId]))
            when {
                request.url.encodedPath.endsWith("/auth/refresh") ->
                    respond("""{"accessToken":"new-access","refreshToken":"new-refresh"}""", headers = json)
                request.headers[HttpHeaders.Authorization] == "Bearer old-access" ->
                    respond("", HttpStatusCode.Unauthorized)
                else -> respond("""{"places":[]}""", headers = json)
            }
        }
        createHttpClient(HttpEngineFactory { engine }, httpLoggingEnabled = true, log = { messages.trySend(it) }).use { http ->
            DozyClient(http, DozyClientConfig("https://example.test", authDiagnostics = { authTrace.add(it) }), storage).use { sdk ->
                val response = sdk.assetHttpClient.post("https://example.test/api/v1/places?cursor=private-value") {
                    contentType(ContentType.Application.Json)
                    setBody("""{"name":"Forest"}""")
                }
                assertEquals(HttpStatusCode.OK, response.status)
                assertEquals("""{"places":[]}""", response.bodyAsText())
                lines = List(11) { messages.receive() }
            }
        }

        assertEquals(
            listOf(
                "POST /api/v1/places: 401; checking session refresh",
                "POST /api/v1/places: refresh started",
                "POST /api/v1/places: refresh succeeded",
                "POST /api/v1/places: retrying original request",
            ),
            authTrace,
        )
        val attempts = lines.filter { it.startsWith("->") || it.startsWith("<-") }
        assertEquals(6, attempts.size)
        assertEquals(3, requestIds.distinct().size)
        assertEquals(requestIds, attempts.filter { it.startsWith("->") }.map { it.substringAfter('[').substringBefore(']') })
        assertEquals(requestIds, attempts.filter { it.startsWith("<-") }.map { it.substringAfter('[').substringBefore(']') })
        assertTrue(attempts[1].contains("401 POST"))
        assertTrue(attempts[2].contains("POST https://example.test:443/api/v1/auth/refresh") ||
            attempts[2].contains("POST https://example.test/api/v1/auth/refresh"))
        assertTrue(attempts[5].contains("200 POST"))
        assertTrue(lines.any { it.contains("response body") && it.contains("""{"places":[]}""") })
        assertTrue(lines.any { it.contains("response body") && it.contains("[redacted]") })
        assertTrue(lines.any { it.contains("request body") && it.contains("""{"name":"Forest"}""") })
        for (secret in listOf("old-access", "old-refresh", "new-access", "new-refresh", "private-value")) {
            assertFalse(lines.any { secret in it })
        }
    }
}
