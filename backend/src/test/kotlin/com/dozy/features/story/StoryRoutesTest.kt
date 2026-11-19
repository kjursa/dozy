package com.dozy.features.story

import com.dozy.asOwner
import com.dozy.api.contracts.v1.ApiErrorResponse
import com.dozy.api.contracts.v1.story.GenerationAcceptedDto
import com.dozy.api.contracts.v1.story.GenerationStatusDto
import com.dozy.api.contracts.v1.story.StoryDto
import com.dozy.databaseConfig
import com.dozy.module
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import java.net.URI
import java.nio.file.Path
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.junit.jupiter.api.io.TempDir

class StoryRoutesTest {
    @TempDir lateinit var directory: Path
    private val owner = "019a0000-0000-7000-8000-000000000010"
    private val stranger = "019a0000-0000-7000-8000-000000000011"
    private val request = """{"cast":[{"id":"019a0000-0000-7000-8000-000000000001","name":"Luna","personality":"Kind","appearance":"Silver coat","type":"ANIMAL"}],"mode":"MANUAL","length":"SHORT","place":{"id":"bundled:moon","name":"Moon garden","description":"Quiet","origin":"SYSTEM","editable":false},"idea":"  Find a star  "}"""
    private val json = Json { ignoreUnknownKeys = true }
    private suspend fun HttpClient.start(body: String = request, key: String = UUID.randomUUID().toString()) = post("/api/v1/story-generations") {
        asOwner(owner); header("Idempotency-Key", key)
        contentType(ContentType.Application.Json); setBody(body)
    }
    private suspend fun HttpClient.read(path: String, installation: String = owner) = get(path) { asOwner(installation) }

    @Test fun `listing defaults empty and rejects invalid limits and cursors`() = testApplication {
        environment { config = databaseConfig(directory) }
        application { module() }
        assertEquals("{\"stories\":[],\"nextCursor\":null}", client.read("/api/v1/stories").bodyAsText())
        for (limit in listOf("0", "101", "abc", "1.5", "", "-1", "1&limit=2")) {
            val response = client.read("/api/v1/stories?limit=$limit")
            assertEquals(ApiErrorResponse("validation.limit.invalid_value"), json.decodeFromString(response.bodyAsText()))
        }
        for (cursor in listOf("", "unknown", "one&cursor=two")) {
            assertEquals("INVALID_CURSOR", json.decodeFromString<ApiErrorResponse>(client.read("/api/v1/stories?cursor=$cursor").bodyAsText()).code)
        }
    }
    @Test fun `retry route uses durable same job and typed errors`() = testApplication {
        environment { config = databaseConfig(directory).apply { put("dozy.generation.textMode", "FAIL_ONCE") } }
        application { module() }
        val original = UUID.randomUUID().toString()
        val job = json.decodeFromString<GenerationAcceptedDto>(client.start(key = original).bodyAsText())
        suspend fun retry(key: String, id: String = job.jobId, installation: String = owner) = client.post("/api/v1/story-generations/$id/retry") {
            asOwner(installation)
            header("Idempotency-Key", key)
        }
        suspend fun awaitStage(stage: String) = withTimeout(10000) {
            do {
                val current = json.decodeFromString<GenerationStatusDto>(client.read("/api/v1/story-generations/${job.jobId}").bodyAsText())
                if (current.status == stage) break
                delay(20)
            } while (true)
        }
        awaitStage("FAILED")
        assertEquals("IDEMPOTENCY_CONFLICT", json.decodeFromString<ApiErrorResponse>(retry(original).bodyAsText()).code)
        assertEquals("JOB_NOT_FOUND", json.decodeFromString<ApiErrorResponse>(retry(UUID.randomUUID().toString(), installation = stranger).bodyAsText()).code)
        val key = UUID.randomUUID().toString()
        val response = retry(key)
        assertEquals(HttpStatusCode.Accepted, response.status)
        assertEquals(job.jobId, json.decodeFromString<GenerationAcceptedDto>(response.bodyAsText()).jobId)
        awaitStage("READY")
        assertEquals(GenerationAcceptedDto(job.jobId, "READY"), json.decodeFromString(retry(key).bodyAsText()))
        assertEquals("JOB_NOT_FAILED", json.decodeFromString<ApiErrorResponse>(retry(UUID.randomUUID().toString()).bodyAsText()).code)
        assertEquals("IDEMPOTENCY_CONFLICT", json.decodeFromString<ApiErrorResponse>(retry(key, "other").bodyAsText()).code)
    }

    @Test fun `full route flow canonical replay immutable read and asset isolation`() = testApplication {
        environment { config = databaseConfig(directory) }
        application { module() }
        val key = UUID.randomUUID().toString()
        val accepted = client.start(key = key)
        assertEquals(HttpStatusCode.Accepted, accepted.status)
        val job = json.decodeFromString<GenerationAcceptedDto>(accepted.bodyAsText())
        val status = withTimeout(10000) {
            var observed: GenerationStatusDto
            do {
                observed = json.decodeFromString<GenerationStatusDto>(client.read("/api/v1/story-generations/${job.jobId}").bodyAsText())
                if (observed.status != "READY") delay(20)
            } while (observed.status != "READY")
            observed
        }
        val reordered = Json.parseToJsonElement(request).let { (it as JsonObject).entries.reversed().associate { it.toPair() } }
        val replay = client.start(JsonObject(reordered).toString(), key)
        assertEquals(GenerationAcceptedDto(job.jobId, "READY"), json.decodeFromString(replay.bodyAsText()))
        val conflict = client.start(request.replace("Find a star", "Find the moon"), key)
        assertEquals("IDEMPOTENCY_CONFLICT", json.decodeFromString<ApiErrorResponse>(conflict.bodyAsText()).code)
        val response = client.read("/api/v1/stories/${status.storyId}")
        assertEquals(HttpStatusCode.OK, response.status)
        val story = json.decodeFromString<StoryDto>(response.bodyAsText())
        assertEquals("Find a star", story.configuration.idea)
        assertTrue(story.text.contains("Luna") && story.text.contains("Moon garden"))
        assertTrue(story.createdAt > 0)
        assertEquals(story, json.decodeFromString<StoryDto>(client.read("/api/v1/stories/${story.id}").bodyAsText()))
        assertEquals("JOB_NOT_FOUND", json.decodeFromString<ApiErrorResponse>(client.read("/api/v1/story-generations/${job.jobId}", stranger).bodyAsText()).code)
        assertEquals("STORY_NOT_FOUND", json.decodeFromString<ApiErrorResponse>(client.read("/api/v1/stories/${story.id}", stranger).bodyAsText()).code)
        val path = URI(story.coverUrl).path
        assertEquals("image/png", client.read(path).headers["Content-Type"]?.substringBefore(';'))
        assertEquals("ASSET_NOT_FOUND", json.decodeFromString<ApiErrorResponse>(client.read(path, stranger).bodyAsText()).code)
        assertEquals(HttpStatusCode.Unauthorized, client.get(path).status)
    }
    @Test fun `structural errors domain validation historical text and null versus omitted binding`() = testApplication {
        environment { config = databaseConfig(directory) }
        application { module() }
        for (body in listOf(
            request.replace("MANUAL", "UNKNOWN"), request.replace("SHORT", "MEDIUM"), request.replace("ANIMAL", "ALIEN"),
            request.replace("MANUAL", "AUTOMATIC"), request.replace("\"Luna\"", "null"), request.replace("\"Kind\"", "false"),
        )) assertEquals(HttpStatusCode.BadRequest, client.start(body).status)
        assertEquals(HttpStatusCode.BadRequest, client.start(key = owner).status)
        val empty = request.replace(Regex("\\[.*?]"), "[]")
        val error = json.decodeFromString<ApiErrorResponse>(client.start(empty).bodyAsText())
        assertEquals(ApiErrorResponse("validation.cast.required"), error)
        val longIdea = client.start(request.replace("  Find a star  ", "x".repeat(1001)))
        assertEquals(ApiErrorResponse("validation.idea.too_long"), json.decodeFromString(longIdea.bodyAsText()))
        val key = UUID.randomUUID().toString()
        val historical = request.replace("Luna", "L".repeat(300)).replace("Kind", "K".repeat(3000))
        assertEquals(HttpStatusCode.Accepted, client.start(historical, key).status)
        val withNull = historical.dropLast(1) + ",\"ending\":null}"
        assertEquals("IDEMPOTENCY_CONFLICT", json.decodeFromString<ApiErrorResponse>(client.start(withNull, key).bodyAsText()).code)
    }
}
