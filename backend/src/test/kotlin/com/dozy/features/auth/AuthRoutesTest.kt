package com.dozy.features.auth

import com.dozy.databaseConfig
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.io.TempDir
import com.dozy.module

class AuthRoutesTest {
    @TempDir lateinit var directory: Path

    @Test
    fun anonymousEndpointCreatesOwnerId() = testApplication {
        environment { config = databaseConfig(directory) }
        application { module() }
        val response = client.post("/api/v1/auth/anonymous")
        assertEquals(HttpStatusCode.Created, response.status)
        val ownerId = Json.parseToJsonElement(response.bodyAsText()).jsonObject.getValue("ownerId").jsonPrimitive.content
        assertEquals(36, ownerId.length)
        assertEquals('7', ownerId[14])
    }

    @Test
    fun emailCodeVerificationRefreshAndLogoutManageSession() = testApplication {
        environment { config = databaseConfig(directory) }
        application { module() }
        val session = client.post("/api/v1/auth/email") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"Parent@Example.com"}""")
        }
        assertEquals(HttpStatusCode.Accepted, session.status)
        val sessionId = Json.parseToJsonElement(session.bodyAsText()).jsonObject.getValue("sessionId").jsonPrimitive.content
        val login = client.post("/api/v1/auth/email/code") {
            contentType(ContentType.Application.Json)
            setBody("""{"sessionId":"$sessionId","code":"123456789"}""")
        }
        assertEquals(HttpStatusCode.OK, login.status)
        val fields = Json.parseToJsonElement(login.bodyAsText()).jsonObject
        val ownerId = fields.getValue("ownerId").jsonPrimitive.content
        val refresh = fields.getValue("refreshToken").jsonPrimitive.content
        val refreshed = client.post("/api/v1/auth/refresh") {
            contentType(ContentType.Application.Json)
            setBody("""{"ownerId":"$ownerId","refreshToken":"$refresh"}""")
        }
        assertEquals(HttpStatusCode.OK, refreshed.status)
        val rotated = Json.parseToJsonElement(refreshed.bodyAsText()).jsonObject.getValue("refreshToken").jsonPrimitive.content
        assertEquals(HttpStatusCode.Unauthorized, client.post("/api/v1/auth/refresh") {
            contentType(ContentType.Application.Json)
            setBody("""{"ownerId":"$ownerId","refreshToken":"$refresh"}""")
        }.status)
        assertEquals(HttpStatusCode.NoContent, client.post("/api/v1/auth/logout") {
            contentType(ContentType.Application.Json)
            setBody("""{"ownerId":"$ownerId"}""")
        }.status)
        assertEquals(HttpStatusCode.Unauthorized, client.post("/api/v1/auth/refresh") {
            contentType(ContentType.Application.Json)
            setBody("""{"ownerId":"$ownerId","refreshToken":"$rotated"}""")
        }.status)
    }

    @Test
    fun emailCodeAllowsOnlyThreeAttemptsAndFiveRequestsPerHour() = testApplication {
        environment { config = databaseConfig(directory) }
        application { module() }
        var lastSession = ""
        repeat(5) {
            val response = client.post("/api/v1/auth/email") {
                contentType(ContentType.Application.Json)
                setBody("""{"email":"limit@example.com"}""")
            }
            assertEquals(HttpStatusCode.Accepted, response.status)
            lastSession = Json.parseToJsonElement(response.bodyAsText()).jsonObject.getValue("sessionId").jsonPrimitive.content
        }
        assertEquals(HttpStatusCode.TooManyRequests, client.post("/api/v1/auth/email") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"limit@example.com"}""")
        }.status)
        repeat(3) { attempt ->
            val result = client.post("/api/v1/auth/email/code") {
                contentType(ContentType.Application.Json)
                setBody("""{"sessionId":"$lastSession","code":"000000000"}""")
            }
            assertEquals(if (attempt == 2) HttpStatusCode.TooManyRequests else HttpStatusCode.Unauthorized, result.status)
        }
    }
}
