package com.dozy.api.client

import com.dozy.api.client.characteravatar.v1.AvatarStartError
import com.dozy.api.client.characteravatar.v1.AvatarStatusError
import com.dozy.api.contracts.v1.characteravatar.AvatarRequestDto
import com.dozy.api.contracts.v1.characteravatar.AvatarField
import com.dozy.api.contracts.v1.ValidationReason
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertFailsWith

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
            assertEquals("QUEUED", (sdk(http).start(input) as ApiResult.Success).value.status)
            assertEquals(1, requests)
            assertEquals("GENERATION_FAILED", (sdk(http).status("job") as ApiResult.Success).value.error)
            assertEquals(2, requests)
        } finally { http.close() }
    }

    @Test fun errorsUseCodesAndUnknownDetailsRemainUnknown() = runTest {
        for ((body, expected) in listOf(
            """{"code":"VALIDATION_FAILED","field":"type","reason":"INVALID_VALUE"}""" to AvatarStartError.Validation(AvatarField.TYPE, ValidationReason.INVALID_VALUE),
            """{"code":"UNAVAILABLE"}""" to AvatarStartError.Unavailable,
        )) {
            val http = HttpClient(MockEngine { respond(body, HttpStatusCode.Conflict) })
            try { assertEquals(ApiResult.Failure(expected), sdk(http).start(input)) } finally { http.close() }
        }
        for (body in listOf(
            """{"code":"FUTURE"}""",
            """{"code":"VALIDATION_FAILED","field":"type","reason":"FUTURE"}""",
            """{"code":"VALIDATION_FAILED","field":"name","reason":"REQUIRED"}""",
        )) {
            val http = HttpClient(MockEngine { respond(body, HttpStatusCode.BadRequest) })
            try { assertIs<AvatarStartError.Unknown>((sdk(http).start(input) as ApiResult.Failure).error) } finally { http.close() }
        }
        val http = HttpClient(MockEngine { respond("""{"code":"AVATAR_JOB_NOT_FOUND"}""", HttpStatusCode.Conflict) })
        try { assertEquals(ApiResult.Failure(AvatarStatusError.NotFound), sdk(http).status("missing")) } finally { http.close() }
    }

    @Test fun structuralErrorsMalformedCombinationsAndCancellationThrow() = runTest {
        val invalid = HttpClient(MockEngine { respond("""{"code":"INVALID_REQUEST"}""", HttpStatusCode.BadRequest) })
        try { assertFailsWith<InvalidRequestException> { sdk(invalid).start(input) } } finally { invalid.close() }
        for (body in listOf(
            "{}", "not json",
            """{"jobId":"job","status":"READY"}""",
            """{"jobId":"job","status":"FAILED"}""",
            """{"jobId":"job","status":"RUNNING","error":"GENERATION_FAILED"}""",
            """{"jobId":"other","status":"QUEUED"}""",
        )) {
            val http = HttpClient(MockEngine { respond(body) })
            try { assertFailsWith<InvalidResponseException> { sdk(http).status("job") } } finally { http.close() }
        }
        val cancelled = HttpClient(MockEngine { throw CancellationException() })
        try { assertFailsWith<CancellationException> { sdk(cancelled).start(input) } } finally { cancelled.close() }
    }
}
