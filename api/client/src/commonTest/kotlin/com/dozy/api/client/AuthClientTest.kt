package com.dozy.api.client

import com.dozy.api.client.auth.v1.RequestEmailCodeResult
import com.dozy.api.client.auth.v1.VerifyEmailCodeResult
import com.dozy.api.client.auth.InMemoryAuthSessionStorage
import com.dozy.api.client.auth.StoredAuthSession
import com.dozy.api.client.auth.SessionState
import com.dozy.api.client.place.v1.PlacesListResult
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.http.content.TextContent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class AuthClientTest {
    @Test
    fun requestEmailCodeUsesAuthV1PathAndJson() = runTest {
        val http = HttpClient(MockEngine { request ->
            assertEquals("/api/v1/auth/email", request.url.encodedPath)
            val body = assertIs<TextContent>(request.body)
            assertEquals("application/json", body.contentType.toString())
            assertEquals("""{"email":"parent@example.com"}""", body.text)
            respond("""{"sessionId":"session-1"}""", HttpStatusCode.Accepted, headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()))
        })
        try {
            val client = DozyClient(http, DozyClientConfig("https://api.example"), InstallationIdProvider { "installation" })
            val result = client.auth.v1.requestEmailCode("parent@example.com")
            assertEquals("session-1", assertIs<RequestEmailCodeResult.Success>(result).sessionId)
        } finally { http.close() }
    }

    @Test
    fun successfulLoginThroughAuthClientStoresSessionInternally() = runTest {
        val storage = InMemoryAuthSessionStorage()
        val http = HttpClient(MockEngine {
            respond("""{"ownerId":"owner-1","accessToken":"access-1","refreshToken":"refresh-1"}""", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()))
        })
        val client = DozyClient(http, DozyClientConfig("https://api.example"), InstallationIdProvider { "installation" }, storage)
        try {
            val result = client.auth.v1.verifyEmailCode("session-1", "123456789")
            assertIs<VerifyEmailCodeResult.Success>(result)
            assertEquals("refresh-1", storage.load()?.refreshToken)
            client.sessionManager.initialize()
            assertEquals(SessionState.Authenticated("owner-1"), client.sessionManager.state.value)
        } finally { client.close(); http.close() }
    }

    @Test
    fun featureRequestRefreshesAndPersistsTokensAfterUnauthorizedResponse() = runTest {
        val storage = InMemoryAuthSessionStorage().apply {
            save(StoredAuthSession("owner-1", "old-access", "old-refresh"))
        }
        var placesCalls = 0
        var refreshCalls = 0
        val http = HttpClient(MockEngine { request ->
            when (request.url.encodedPath) {
                "/api/v1/places" -> {
                    placesCalls++
                    if (placesCalls == 1) {
                        assertEquals("Bearer old-access", request.headers[HttpHeaders.Authorization])
                    }
                    if (placesCalls == 1) respond("""{"code":"UNAUTHORIZED"}""", HttpStatusCode.Unauthorized)
                    else {
                        assertEquals("Bearer new-access", request.headers[HttpHeaders.Authorization])
                        respond("""{"places":[]}""", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()))
                    }
                }
                "/api/v1/auth/refresh" -> {
                    refreshCalls++
                    assertEquals("""{"ownerId":"owner-1","refreshToken":"old-refresh"}""", assertIs<TextContent>(request.body).text)
                    respond("""{"accessToken":"new-access","refreshToken":"new-refresh"}""", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()))
                }
                else -> error("Unexpected path ${request.url.encodedPath}")
            }
        })
        val client = DozyClient(http, DozyClientConfig("https://api.example"), InstallationIdProvider { "installation" }, storage)
        try {
            assertIs<PlacesListResult.Success>(client.places.v1.list())
            assertEquals(2, placesCalls)
            assertEquals(1, refreshCalls)
            assertEquals("new-refresh", storage.load()?.refreshToken)
        } finally { client.close(); http.close() }
    }

    @Test
    fun guestSessionAuthorizesOnlyAssetsOnTheApiOrigin() = runTest {
        val storage = InMemoryAuthSessionStorage()
        val http = HttpClient(MockEngine { request ->
            when {
                request.url.encodedPath.endsWith("/auth/anonymous") ->
                    respond("""{"ownerId":"guest","accessToken":"guest-access","refreshToken":"guest-refresh"}""", HttpStatusCode.Created)
                request.url.host == "api.example" -> {
                    assertEquals("Bearer guest-access", request.headers[HttpHeaders.Authorization])
                    respond("image")
                }
                else -> {
                    assertEquals(null, request.headers[HttpHeaders.Authorization])
                    respond("unauthorized", HttpStatusCode.Unauthorized)
                }
            }
        })
        val client = DozyClient(http, DozyClientConfig("https://api.example"), InstallationIdProvider { "installation" }, storage)
        try {
            client.auth.v1.anonymous()
            client.sessionManager.initialize()
            assertEquals(SessionState.Anonymous("guest"), client.sessionManager.state.value)
            assertEquals(HttpStatusCode.OK, client.assetHttpClient.get("https://api.example/assets/avatar").status)
            assertEquals(HttpStatusCode.Unauthorized, client.assetHttpClient.get("https://other.example/assets/avatar").status)
        } finally {
            client.close()
            http.close()
        }
    }

    @Test
    fun rejectedRefreshClearsSessionAndEmitsExpiredState() = runTest {
        val storage = InMemoryAuthSessionStorage()
        val expired = StoredAuthSession("owner-1", "expired-access", "revoked-refresh")
        storage.save(expired)
        val http = HttpClient(MockEngine {
            respond("""{"code":"INVALID_REFRESH_TOKEN"}""", HttpStatusCode.Unauthorized)
        })
        val client = DozyClient(http, DozyClientConfig("https://api.example"), InstallationIdProvider { "installation" }, storage)
        try {
            client.sessionManager.initialize()
            assertEquals(null, client.sessionManager.refreshForRequest(expired))
            assertEquals(SessionState.Expired, client.sessionManager.state.value)
            assertEquals(null, storage.load())
        } finally {
            client.close()
            http.close()
        }
    }
}
