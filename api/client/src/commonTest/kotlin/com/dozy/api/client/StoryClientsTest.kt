package com.dozy.api.client

import com.dozy.api.client.story.v1.StoryListResult
import com.dozy.api.client.story.v1.StoryReadResult
import com.dozy.api.client.story.v1.StorySyncResult
import com.dozy.api.client.storygeneration.v1.GenerationRetryResult
import com.dozy.api.client.storygeneration.v1.GenerationStartResult
import com.dozy.api.client.storygeneration.v1.GenerationStatusResult
import com.dozy.api.contracts.v1.character.CharacterDto
import com.dozy.api.contracts.v1.story.GenerationAcceptedDto
import com.dozy.api.contracts.v1.story.GenerationRequestDto
import com.dozy.api.contracts.v1.story.ListStoriesRequest
import com.dozy.api.contracts.v1.story.StorySyncError
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
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

class StoryClientsTest {
    private val input = GenerationRequestDto(listOf(CharacterDto("hero", "Luna", "Kind", "Coat", "ANIMAL")), "AUTOMATIC", "SHORT", idea = "hidden", ending = "hidden")
    private fun sdk(http: HttpClient) = DozyClient(http, DozyClientConfig("https://example.test/service"))

    @Test fun `listing sends the story ID cursor`() = runTest {
        val http = HttpClient(MockEngine { request ->
            assertEquals("/service/api/v1/stories", request.url.encodedPath)

            assertEquals("story-019a0000-0000-7000-8000-000000000001", request.url.parameters["cursor"])
            assertEquals("2", request.url.parameters["limit"])
            respond("""{"stories":[],"nextCursor":null}""")
        })
        try { assertEquals(emptyList(), assertIs<StoryListResult.Success>(sdk(http).stories.v1.list(ListStoriesRequest(2, "story-019a0000-0000-7000-8000-000000000001"))).response.stories) } finally { http.close() }
    }

    @Test fun `sync reads the requested revision and decodes only summaries and deleted IDs`() =
        runTest {
            val http =
                HttpClient(
                    MockEngine { request ->
                        assertEquals("/service/api/v1/stories/sync", request.url.encodedPath)
                        when (request.url.parameters["after"]) {
                            "4" ->
                                respond(
                                    """{"upserts":[{"id":"new","title":"Title","coverUrl":"https://example.test/cover","readingMinutes":2,"createdAt":123}],"deletedIds":["old"],"nextRevision":7}""",
                                )
                            "7" -> respond("""{"upserts":[],"deletedIds":["old"],"nextRevision":7}""")
                            else -> respond("""{"code":"INVALID_CURSOR"}""", HttpStatusCode.BadRequest)
                        }
                    },
                )
            try {
                val client = sdk(http).stories.v1
                val changes = assertIs<StorySyncResult.Success>(client.sync(4)).response
                assertEquals("new", changes.upserts.single().id)
                assertEquals(listOf("old"), changes.deletedIds)
                assertEquals(7L, changes.nextRevision)
                assertIs<StorySyncResult.TechnicalError>(client.sync(7))
                assertEquals(StorySyncResult.ApiError(StorySyncError.InvalidRevision), client.sync(0))
            } finally {
                http.close()
            }
        }

    @Test fun `retry sends no body and maps contract codes independently of status`() = runTest {
        val http = HttpClient(MockEngine { request ->
            assertEquals("/service/api/v1/story-generations/job/retry", request.url.encodedPath)
            assertEquals("POST", request.method.value)

            assertEquals("retry-key", request.headers["Idempotency-Key"])
            assertEquals(0L, request.body.contentLength)
            respond("""{"jobId":"job","status":"FAILED"}""", HttpStatusCode.Accepted)
        })
        try {
            assertEquals(GenerationRetryResult.Success(GenerationAcceptedDto("job", "FAILED")), sdk(http).storyGenerations.v1.retry("job", "retry-key"))
        } finally { http.close() }
    }

    @Test fun `versioned containers submit snapshot omission key identity and retrieve status and story`() = runTest {
        var calls = 0
        val story = """{"id":"story","jobId":"job","title":"Luna","text":"Story text","coverUrl":"https://example.test/cover","createdAt":123,"configuration":{"cast":[{"id":"hero","name":"Luna","personality":"Kind","appearance":"Coat","type":"ANIMAL"}],"mode":"AUTOMATIC","length":"SHORT"}}"""
        val http = HttpClient(MockEngine { request ->

            when (calls++) {
                0 -> {
                    assertEquals("/service/api/v1/story-generations", request.url.encodedPath)
                    assertEquals("key", request.headers["Idempotency-Key"])
                    assertEquals(setOf("cast", "mode", "length"), Json.parseToJsonElement((request.body as TextContent).text).jsonObject.keys)
                    respond("""{"jobId":"job","status":"READY","future":true}""", HttpStatusCode.Accepted)
                }
                1 -> {
                    assertEquals("/service/api/v1/story-generations/job", request.url.encodedPath)
                    respond("""{"jobId":"job","status":"READY","storyId":"story","story":$story}""")
                }
                else -> {
                    assertEquals("/service/api/v1/stories/story", request.url.encodedPath)
                    respond(story)
                }
            }
        })
        try {
            assertEquals("job", assertIs<GenerationStartResult.Success>(sdk(http).storyGenerations.v1.start("key", input)).response.let { (it as GenerationAcceptedDto).jobId })
            val ready = assertIs<GenerationStatusResult.Success>(sdk(http).storyGenerations.v1.status("job"))
            val read = assertIs<StoryReadResult.Success>(sdk(http).stories.v1.read("story"))
            assertEquals(read.response, ready.response.story)
        } finally { http.close() }
    }
    @Test fun `malformed terminal states become technical errors and cancellation propagates`() = runTest {
        for (code in listOf("INVALID_REQUEST", "INVALID_OWNER_ID")) {
            val http = HttpClient(MockEngine { respond("""{"code":"$code"}""", HttpStatusCode.BadRequest) })
            try { assertIs<GenerationStatusResult.TechnicalError>(sdk(http).storyGenerations.v1.status("job")) } finally { http.close() }
        }
        for (body in listOf(
            """{"jobId":"job","status":"READY"}""",
            """{"jobId":"job","status":"READY","storyId":"story"}""",
            """{"jobId":"job","status":"FAILED"}""",
            """{"jobId":"job","status":"QUEUED","storyId":"story"}""",
            """{"jobId":"job","status":"FAILED","errorCode":"FUTURE"}""",
        )) {
            val http = HttpClient(MockEngine { respond(body) })
            try { assertIs<GenerationStatusResult.TechnicalError>(sdk(http).storyGenerations.v1.status("job")) } finally { http.close() }
        }
        val broken = HttpClient(MockEngine { respond("{}") })
        try { assertIs<GenerationStatusResult.TechnicalError>(sdk(broken).storyGenerations.v1.status("job")) } finally { broken.close() }
        val cancelled = HttpClient(MockEngine { throw CancellationException("cancel") })
        try { assertFailsWith<CancellationException> { sdk(cancelled).storyGenerations.v1.start("key", input) } } finally { cancelled.close() }
        val offline = HttpClient(MockEngine { throw IllegalStateException("offline") })
        try { assertIs<StoryReadResult.TechnicalError>(sdk(offline).stories.v1.read("story")) } finally { offline.close() }
    }
}
