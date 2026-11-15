package com.dozy.mobile

import com.dozy.api.client.DozyClient
import com.dozy.api.client.DozyClientConfig
import com.dozy.api.client.InstallationIdProvider
import com.dozy.mobile.core.config.ApiConfig
import com.dozy.mobile.core.data.api.HttpEngineFactory
import com.dozy.mobile.core.data.api.createHttpClient
import com.dozy.mobile.feature.story.data.DefaultGenerationRepository
import com.dozy.mobile.feature.story.data.api.StoryPageDto
import com.dozy.mobile.feature.story.domain.model.GenerationJob
import com.dozy.mobile.feature.story.domain.model.Story
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json

class StorySdkIntegrationTest {

    @Test fun `SDK retry reconciles persisted key before polling and later failure gets a fresh key`() =
        runTest {
            val dao = FakeGenerationDao()
            val configuration = ProgressApi().configuration
            val keys = mutableListOf<String>()
            var lost = true
            var polls = 0
            val http =
                HttpClient(
                    MockEngine { request ->
                        assertEquals("installation", request.headers["X-Installation-Id"])
                        when (request.url.encodedPath) {
                            "/api/v1/story-generations" -> respond("""{"jobId":"job","status":"QUEUED"}""", HttpStatusCode.Accepted)
                            "/api/v1/story-generations/job/retry" -> {
                                keys += requireNotNull(request.headers["Idempotency-Key"])
                                if (lost) error("Lost acceptance")
                                respond("""{"jobId":"job","status":"FAILED"}""", HttpStatusCode.Accepted)
                            }
                            else -> {
                                polls++
                                respond("""{"jobId":"job","status":"FAILED","errorCode":"GENERATION_FAILED"}""")
                            }
                        }
                    },
                )
            val sdk = DozyClient(http, DozyClientConfig("https://example.test"), InstallationIdProvider { "installation" })

            fun repository() = DefaultGenerationRepository(dao, sdk.storyGenerations, sdk.stories)
            val id = GenerationJob.Id("job")
            try {
                repository().start(GenerationJob.RequestKey("create"), configuration)
                repository().refresh(id)
                val snapshot =
                    dao.rows.value
                        .single()
                        .configuration
                try {
                    repository().retry(id, GenerationJob.RequestKey("retry-one"))
                } catch (_: IllegalStateException) {
                }
                assertEquals(
                    "retry-one",
                    dao.rows.value
                        .single()
                        .retryKey,
                )
                // A status poll must not clear acceptance uncertainty, even for a terminal state.
                repository().refresh(id)
                assertEquals(1, polls)
                assertEquals(
                    "retry-one",
                    dao.rows.value
                        .single()
                        .retryKey,
                )
                lost = false
                repository().retry(id, GenerationJob.RequestKey("unused"))
                assertEquals(listOf("retry-one", "retry-one"), keys)
                assertNull(
                    dao.rows.value
                        .single()
                        .retryKey,
                )
                assertTrue(
                    dao.rows.value
                        .single()
                        .statusUnknown,
                )
                repository().refresh(id)
                repository().retry(id, GenerationJob.RequestKey("retry-two"))
                assertEquals(listOf("retry-one", "retry-one", "retry-two"), keys)
                assertEquals(
                    "job",
                    dao.rows.value
                        .single()
                        .jobId,
                )
                assertEquals(
                    snapshot,
                    dao.rows.value
                        .single()
                        .configuration,
                )
            } finally {
                http.close()
            }
        }

    @Test fun `shared listing traverses pages with identity and accepts configured local covers without generation`() = runTest {
        val cursors = mutableListOf<String?>()
        val http = HttpClient(MockEngine { request ->
            assertEquals(HttpMethod.Get, request.method)
            assertEquals("installation", request.headers["X-Installation-Id"])
            assertEquals("/api/v1/stories", request.url.encodedPath)
            assertEquals("100", request.url.parameters["limit"])
            val cursor = request.url.parameters["cursor"]
            cursors += cursor
            val id = if (cursor == null) "a" else "b"
            val next = if (cursor == null) "\"opaque /+\"" else "null"
            respond("""{"stories":[{"id":"$id","title":"Title","coverUrl":"http://localhost:8080/api/v1/assets/$id","readingMinutes":1,"createdAt":123}],"nextCursor":$next}""")
        })
        try {
            val sdk = DozyClient(http, DozyClientConfig("http://localhost:8080"), InstallationIdProvider { "installation" })
            val repo = DefaultGenerationRepository(FakeGenerationDao(), sdk.storyGenerations, sdk.stories)
            assertTrue(repo.refreshStories())
            assertEquals(listOf(null, "opaque /+"), cursors)
            assertEquals(listOf("a", "b"), repo.summaries().first().map { it.id.value })
        } finally { http.close() }
    }

    @Test fun `SDK polling persists result before ready and recreated repository reads immutable cache`() =
        runTest {
            val dao = FakeGenerationDao()
            val fixture = ProgressApi()
            val configuration = fixture.configuration
            var starts = 0
            var reads = 0
            var unavailableStatus = true
            val bodies = mutableListOf<String>()
            val http =
                HttpClient(
                    MockEngine { request ->
                        assertEquals("installation", request.headers["X-Installation-Id"])
                        when (request.url.encodedPath) {
                            "/api/v1/story-generations" -> {
                                assertEquals("stable", request.headers["Idempotency-Key"])
                                bodies += (request.body as TextContent).text
                                if (++starts == 1) error("Uncertain accepted response")
                                respond("""{"jobId":"job","status":"QUEUED"}""", HttpStatusCode.Accepted)
                            }
                            "/api/v1/story-generations/job" ->
                                if (unavailableStatus) {
                                    unavailableStatus = false
                                    respond("""{"code":"UNAVAILABLE"}""", HttpStatusCode.ServiceUnavailable)
                                } else {
                                    respond("""{"jobId":"job","status":"READY","storyId":"story"}""")
                                }
                            else -> {
                                assertEquals("/api/v1/stories/story", request.url.encodedPath)
                                reads++
                                respond(Json.encodeToString(fixture.story("story")))
                            }
                        }
                    },
                )
            val sdk = DozyClient(http, DozyClientConfig("https://example.test"), InstallationIdProvider { "installation" })

            fun repository() = DefaultGenerationRepository(dao, sdk.storyGenerations, sdk.stories)
            val key = GenerationJob.RequestKey("stable")
            try {
                try {
                    repository().start(key, configuration)
                } catch (_: IllegalStateException) {
                }
                assertEquals(1, dao.rows.value.size)
                assertNull(
                    dao.rows.value
                        .single()
                        .jobId,
                )
                val accepted = repository().start(key, configuration.copy(idea = "edited draft"))
                assertEquals("job", accepted?.id?.value)
                assertEquals(bodies.first(), bodies.last())
                val unknown = repository().refresh(GenerationJob.Id("job"))
                assertEquals(GenerationJob.Status.QUEUED, unknown?.status)
                assertTrue(unknown?.statusUnknown == true)
                val ready = repository().refresh(GenerationJob.Id("job"))
                assertEquals(GenerationJob.Status.READY, ready?.status)
                assertTrue(
                    dao.rows.value
                        .single()
                        .result != null,
                )
                assertEquals(1, reads)
                val first = repository().readStory(Story.Id("story"))
                val restored = repository().readStory(Story.Id("story"))
                assertEquals(first, restored)
                assertEquals(1, reads)
                assertEquals(1, repository().stories().first().size)
            } finally {
                http.close()
            }
        }

    @Test fun `image requests send installation header only to configured asset origin and path`() =
        runTest {
            var calls = 0
            val engine =
                MockEngine { request ->
                    assertEquals(if (calls++ == 0) "installation" else null, request.headers["X-Installation-Id"])
                    respond("bytes")
                }
            val http =
                createHttpClient(
                    HttpEngineFactory { engine },
                    ApiConfig("http://localhost:8080/service"),
                    InstallationIdProvider { "installation" },
                )
            try {
                http.get("http://localhost:8080/service/api/v1/assets/cover")
                http.get("https://third-party.test/service/api/v1/assets/cover")
                http.get("http://localhost:8080/unrelated/cover")
                http.get("http://localhost:8081/service/api/v1/assets/cover")
                http.get("https://localhost:8080/service/api/v1/assets/cover")
            } finally {
                http.close()
            }
        }
}
