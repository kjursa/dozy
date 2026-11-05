package com.dozy

import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals

class ApplicationTest {
    @Test
    fun helloReturnsMessage() = testApplication {
        application { module() }

        val response = client.get("/hello")

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("application/json", response.headers["Content-Type"]?.substringBefore(';'))
        assertEquals("""{"message":"Hello world from Dozy"}""", response.bodyAsText())
    }
}
