package com.dozy.api.client

import com.dozy.api.client.characteravatar.v1.AvatarStartResult
import com.dozy.api.client.characteravatar.v1.AvatarStatusResult
import com.dozy.api.contracts.v1.characteravatar.AvatarRequestDto
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

class CharacterAvatarsClientTest {
    private val input = AvatarRequestDto("CHILD", "kind", "eyes")
    private fun sdk(http: HttpClient) = DozyClient(http, DozyClientConfig("https://example.test"), InstallationIdProvider { "installation" }).characterAvatars.v1

    @Test fun eachMethodSendsOneRequestAndFailedJobIsSuccess() = runTest {
        var requests = 0
        val http = HttpClient(MockEngine { request ->
            requests++
            assertEquals("installation", request.headers["X-Installation-Id"])
            when (requests) {
                1 -> {
                    assertEquals("POST", request.method.value)
                    assertEquals("/api/v1/character-avatars", request.url.encodedPath)
                    val body = (request.body as TextContent).text
                    assertEquals(input, Json.decodeFromString<AvatarRequestDto>(body))
                    assertEquals(setOf("type", "personality", "appearance"), Json.parseToJsonElement(body).jsonObject.keys)
                    respond("""{"jobId":"job","status":"QUEUED"}""", HttpStatusCode.Accepted)
                }
                else -> {
                    assertEquals("GET", request.method.value)
                    assertEquals("/api/v1/character-avatars/job", request.url.encodedPath)
                    respond("""{"jobId":"job","status":"FAILED","error":"GENERATION_FAILED"}""")
                }
            }
        })
        try {
            assertEquals("QUEUED", (sdk(http).start(input) as AvatarStartResult.Success).response.status)
            assertEquals(1, requests)
            assertEquals("GENERATION_FAILED", (sdk(http).status("job") as AvatarStatusResult.Success).response.error)
            assertEquals(2, requests)
        } finally { http.close() }
    }

    @Test fun malformedTerminalCombinationsAreCommunicationFailures() = runTest {
        for (body in listOf(
            "{}", "not json",
            """{"jobId":"job","status":"READY"}""",
            """{"jobId":"job","status":"FAILED"}""",
            """{"jobId":"job","status":"RUNNING","error":"GENERATION_FAILED"}""",
            """{"jobId":"other","status":"QUEUED"}""",
        )) {
            val http = HttpClient(MockEngine { respond(body) })
            try { assertIs<TechnicalError.Communication>(assertIs<AvatarStatusResult.TechnicalError>(sdk(http).status("job")).error) }
            finally { http.close() }
        }
    }

}
