package com.dozy.plugins

import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import org.slf4j.MDC
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class RequestLoggingTest {
    @Test
    fun correlatesRequestResponseAndLogContext() = testApplication {
        application {
            configureRequestLogging()
            routing {
                get("/probe") {
                    assertEquals(call.response.headers[HttpHeaders.XRequestId], MDC.get("requestId"))
                    assertNotNull(MDC.get("requestId"))
                    call.respond(HttpStatusCode.BadRequest)
                }
            }
        }

        val id = UUID.randomUUID().toString()
        val response = client.get("/probe") { header(HttpHeaders.XRequestId, id) }
        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals(id, response.headers[HttpHeaders.XRequestId])

        val invalid = client.get("/probe") { header(HttpHeaders.XRequestId, "invalid") }
        assertEquals(HttpStatusCode.BadRequest, invalid.status)
        val replacement = assertNotNull(invalid.headers[HttpHeaders.XRequestId])
        assertEquals(replacement, UUID.fromString(replacement).toString())
    }
}
