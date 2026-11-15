package com.dozy.api.client

import com.dozy.api.client.story.v1.StoryReadError
import com.dozy.api.client.storygeneration.v1.GenerationRetryError
import com.dozy.api.client.storygeneration.v1.GenerationStartError
import com.dozy.api.client.storygeneration.v1.GenerationStatusError
import com.dozy.api.contracts.v1.character.CharacterDto
import com.dozy.api.contracts.v1.story.GenerationAcceptedDto
import com.dozy.api.contracts.v1.story.GenerationRequestDto
import com.dozy.api.contracts.v1.story.GenerationField
import com.dozy.api.contracts.v1.ValidationReason
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

class StoryClientsTest {
    private val input = GenerationRequestDto(listOf(CharacterDto("hero", "Luna", "Kind", "Coat", "ANIMAL")), "AUTOMATIC", "SHORT", idea = "hidden", ending = "hidden")
    private fun sdk(http: HttpClient) = DozyClient(http, DozyClientConfig("https://example.test/service"), InstallationIdProvider { "installation" })

    @Test fun `retry sends no body and maps contract codes independently of status`() = runTest {
        val http = HttpClient(MockEngine { request ->
            assertEquals("/service/api/v1/story-generations/job/retry", request.url.encodedPath)
            assertEquals("POST", request.method.value)
            assertEquals("installation", request.headers["X-Installation-Id"])
            assertEquals("retry-key", request.headers["Idempotency-Key"])
            assertEquals(0L, request.body.contentLength)
            respond("""{"jobId":"job","status":"FAILED"}""", HttpStatusCode.Accepted)
        })
        try {
            assertEquals(ApiResult.Success(GenerationAcceptedDto("job", "FAILED")), sdk(http).storyGenerations.v1.retry("job", "retry-key"))
        } finally { http.close() }
        for ((code, expected) in listOf(
            "JOB_NOT_FOUND" to GenerationRetryError.JobNotFound,
            "JOB_NOT_FAILED" to GenerationRetryError.JobNotFailed,
            "ACTIVE_JOB_EXISTS" to GenerationRetryError.ActiveJobExists,
            "IDEMPOTENCY_CONFLICT" to GenerationRetryError.IdempotencyConflict,
            "UNAVAILABLE" to GenerationRetryError.Unavailable,
        )) {
            val errors = HttpClient(MockEngine { respond("""{"code":"$code"}""", HttpStatusCode.InternalServerError) })
            try { assertEquals(ApiResult.Failure(expected), sdk(errors).storyGenerations.v1.retry("job", "key")) }
            finally { errors.close() }
        }
    }

    @Test fun `versioned containers submit snapshot omission key identity and retrieve status and story`() = runTest {
        var calls = 0
        val http = HttpClient(MockEngine { request ->
            assertEquals("installation", request.headers["X-Installation-Id"])
            when (calls++) {
                0 -> {
                    assertEquals("/service/api/v1/story-generations", request.url.encodedPath)
                    assertEquals("key", request.headers["Idempotency-Key"])
                    assertEquals(setOf("cast", "mode", "length"), Json.parseToJsonElement((request.body as TextContent).text).jsonObject.keys)
                    respond("""{"jobId":"job","status":"READY","future":true}""", HttpStatusCode.Accepted)
                }
                1 -> {
                    assertEquals("/service/api/v1/story-generations/job", request.url.encodedPath)
                    respond("""{"jobId":"job","status":"READY","storyId":"story"}""")
                }
                else -> {
                    assertEquals("/service/api/v1/stories/story", request.url.encodedPath)
                    respond("""{"id":"story","title":"Luna","text":"Story text","coverUrl":"https://example.test/cover","createdAt":123,"configuration":{"cast":[{"id":"hero","name":"Luna","personality":"Kind","appearance":"Coat","type":"ANIMAL"}],"mode":"AUTOMATIC","length":"SHORT"}}""")
                }
            }
        })
        try {
            assertEquals("job", assertIs<ApiResult.Success<*>>(sdk(http).storyGenerations.v1.start("key", input)).value.let { (it as GenerationAcceptedDto).jobId })
            assertIs<ApiResult.Success<*>>(sdk(http).storyGenerations.v1.status("job"))
            assertIs<ApiResult.Success<*>>(sdk(http).stories.v1.read("story"))
        } finally { http.close() }
    }
    @Test fun `errors classify by body code and preserve unknown validation combinations`() = runTest {
        val errors = listOf(
            """{"code":"IDEMPOTENCY_CONFLICT"}""" to GenerationStartError.IdempotencyConflict,
            """{"code":"ACTIVE_JOB_EXISTS"}""" to GenerationStartError.ActiveJobExists,
            """{"code":"REFERENCE_UNAVAILABLE"}""" to GenerationStartError.ReferenceUnavailable,
            """{"code":"UNAVAILABLE"}""" to GenerationStartError.Unavailable,
            """{"code":"VALIDATION_FAILED","field":"idea","reason":"TOO_LONG"}""" to GenerationStartError.Validation(GenerationField.IDEA, ValidationReason.TOO_LONG),
        )
        for ((body, expected) in errors) {
            val http = HttpClient(MockEngine { respond(body, HttpStatusCode.InternalServerError) })
            try { assertEquals(ApiResult.Failure(expected), sdk(http).storyGenerations.v1.start("key", input)) } finally { http.close() }
        }
        for (body in listOf("""{"code":"FUTURE"}""", """{"code":"VALIDATION_FAILED","field":"idea","reason":"REQUIRED"}""")) {
            val http = HttpClient(MockEngine { respond(body, HttpStatusCode.BadRequest) })
            try { assertIs<GenerationStartError.Unknown>(assertIs<ApiResult.Failure<*>>(sdk(http).storyGenerations.v1.start("key", input)).error) } finally { http.close() }
        }
        for ((code, expected) in listOf("JOB_NOT_FOUND" to GenerationStatusError.NotFound, "UNAVAILABLE" to GenerationStatusError.Unavailable)) {
            val http = HttpClient(MockEngine { respond("""{"code":"$code"}""", HttpStatusCode.Forbidden) })
            try { assertEquals(ApiResult.Failure(expected), sdk(http).storyGenerations.v1.status("job")) } finally { http.close() }
        }
        val http = HttpClient(MockEngine { respond("""{"code":"STORY_NOT_FOUND"}""", HttpStatusCode.InternalServerError) })
        try { assertEquals(ApiResult.Failure(StoryReadError.NotFound), sdk(http).stories.v1.read("story")) } finally { http.close() }
    }
    @Test fun `invalid requests malformed terminal states transport failures and cancellation remain exceptions`() = runTest {
        for (code in listOf("INVALID_REQUEST", "INVALID_INSTALLATION_ID")) {
            val http = HttpClient(MockEngine { respond("""{"code":"$code"}""", HttpStatusCode.BadRequest) })
            try { assertFailsWith<InvalidRequestException> { sdk(http).storyGenerations.v1.status("job") } } finally { http.close() }
        }
        for (body in listOf(
            """{"jobId":"job","status":"READY"}""",
            """{"jobId":"job","status":"FAILED"}""",
            """{"jobId":"job","status":"QUEUED","storyId":"story"}""",
            """{"jobId":"job","status":"FAILED","errorCode":"FUTURE"}""",
        )) {
            val http = HttpClient(MockEngine { respond(body) })
            try { assertFailsWith<IllegalArgumentException> { sdk(http).storyGenerations.v1.status("job") } } finally { http.close() }
        }
        val broken = HttpClient(MockEngine { respond("{}") })
        try { assertFailsWith<SerializationException> { sdk(broken).storyGenerations.v1.status("job") } } finally { broken.close() }
        val cancelled = HttpClient(MockEngine { throw CancellationException("cancel") })
        try { assertFailsWith<CancellationException> { sdk(cancelled).storyGenerations.v1.start("key", input) } } finally { cancelled.close() }
        val offline = HttpClient(MockEngine { throw IllegalStateException("offline") })
        try { assertFailsWith<IllegalStateException> { sdk(offline).stories.v1.read("story") } } finally { offline.close() }
    }
}
