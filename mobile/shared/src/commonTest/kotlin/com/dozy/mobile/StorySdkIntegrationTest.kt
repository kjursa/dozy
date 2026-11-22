package com.dozy.mobile

import com.dozy.api.client.DozyClient
import com.dozy.api.client.DozyClientConfig
import com.dozy.api.client.auth.InMemoryAuthSessionStorage
import com.dozy.api.client.auth.StoredAuthSession
import com.dozy.api.contracts.v1.story.StorySummaryDto
import com.dozy.api.contracts.v1.story.StorySyncDto
import com.dozy.mobile.core.data.api.HttpEngineFactory
import com.dozy.mobile.core.data.api.createHttpClient
import com.dozy.mobile.feature.story.data.DefaultGenerationRepository
import com.dozy.mobile.feature.story.domain.model.GenerationJob
import com.dozy.mobile.feature.story.domain.model.Story
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

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
            val sdk = DozyClient(http, DozyClientConfig("https://example.test"))

            fun repository() = DefaultGenerationRepository(dao, sdk.storyGenerations, sdk.stories)
            val id = GenerationJob.Id("job")
            try {
                repository().start(GenerationJob.RequestKey("create"), configuration)
                repository().refresh(id)
                val snapshot =
                    dao.rows
                        .single()
                        .configuration
                try {
                    repository().retry(id, GenerationJob.RequestKey("retry-one"))
                } catch (_: IllegalStateException) {
                }
                assertEquals(
                    "retry-one",
                    dao.rows
                        .single()
                        .retryKey,
                )
                // A status poll must not clear acceptance uncertainty, even for a terminal state.
                repository().refresh(id)
                assertEquals(1, polls)
                assertEquals(
                    "retry-one",
                    dao.rows
                        .single()
                        .retryKey,
                )
                lost = false
                repository().retry(id, GenerationJob.RequestKey("unused"))
                assertEquals(listOf("retry-one", "retry-one"), keys)
                assertNull(
                    dao.rows
                        .single()
                        .retryKey,
                )
                assertTrue(
                    dao.rows
                        .single()
                        .statusUnknown,
                )
                repository().refresh(id)
                repository().retry(id, GenerationJob.RequestKey("retry-two"))
                assertEquals(listOf("retry-one", "retry-one", "retry-two"), keys)
                assertEquals(
                    "job",
                    dao.rows
                        .single()
                        .jobId,
                )
                assertEquals(
                    snapshot,
                    dao.rows
                        .single()
                        .configuration,
                )
            } finally {
                http.close()
            }
        }

    @Test fun `story sync uses stored revisions and accepts configured local covers`() =
        runTest {
            val revisions = mutableListOf<String>()
            val cover = "http://localhost:8080/api/v1/assets/"
            val http =
                HttpClient(
                    MockEngine { request ->
                        assertEquals(HttpMethod.Get, request.method)
                        assertEquals("/api/v1/stories/sync", request.url.encodedPath)
                        val revision = requireNotNull(request.url.parameters["after"])
                        revisions += revision
                        val changes =
                            when (revision) {
                                "0" -> StorySyncDto(listOf(StorySummaryDto("a", "First", cover + "a", 1, 123)), emptyList(), 4)
                                "4" -> StorySyncDto(listOf(StorySummaryDto("b", "Second", cover + "b", 1, 456)), emptyList(), 7)
                                else -> error("Unexpected revision $revision")
                            }
                        respond(Json.encodeToString(changes))
                    },
                )
            try {
                val sdk = DozyClient(http, DozyClientConfig("http://localhost:8080"))
                val repo = DefaultGenerationRepository(FakeGenerationDao(), sdk.storyGenerations, sdk.stories)
                assertTrue(repo.refreshStories())
                assertTrue(repo.refreshStories())
                assertEquals(listOf("0", "4"), revisions)
                assertEquals(listOf("b", "a"), repo.summaries().first().map { it.id.value })
            } finally {
                http.close()
            }
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
                                    respond(Json.encodeToString(fixture.ready()))
                                }
                            else -> {
                                assertEquals("/api/v1/stories/story", request.url.encodedPath)
                                reads++
                                respond(Json.encodeToString(fixture.story("story")))
                            }
                        }
                    },
                )
            val sdk = DozyClient(http, DozyClientConfig("https://example.test"))

            fun repository() = DefaultGenerationRepository(dao, sdk.storyGenerations, sdk.stories)
            val key = GenerationJob.RequestKey("stable")
            try {
                try {
                    repository().start(key, configuration)
                } catch (_: IllegalStateException) {
                }
                assertEquals(1, dao.rows.size)
                assertNull(
                    dao.rows
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
                assertTrue(dao.rows.isEmpty())
                assertTrue(dao.findStory("story")?.result != null)
                assertEquals(0, reads)
                val first = repository().readStory(Story.Id("story"))
                val restored = repository().readStory(Story.Id("story"))
                assertEquals(first, restored)
                assertEquals(0, reads)
                assertEquals(1, repository().stories().first().size)
            } finally {
                http.close()
            }
        }

    @Test fun `image requests send bearer credentials only to the configured API origin and path`() =
        runTest {
            var calls = 0
            val engine =
                MockEngine { request ->
                    assertEquals(if (calls++ == 0) "Bearer access" else null, request.headers[HttpHeaders.Authorization])
                    respond("bytes")
                }
            val http = createHttpClient(HttpEngineFactory { engine })
            val storage = InMemoryAuthSessionStorage()
            storage.save(StoredAuthSession("owner", "access", "refresh"))
            try {
                DozyClient(http, DozyClientConfig("http://localhost:8080/service"), storage).use { client ->
                    client.assetHttpClient.get("http://localhost:8080/service/api/v1/assets/cover")
                    client.assetHttpClient.get("https://third-party.test/service/api/v1/assets/cover")
                    client.assetHttpClient.get("http://localhost:8080/unrelated/cover")
                    client.assetHttpClient.get("http://localhost:8081/service/api/v1/assets/cover")
                    client.assetHttpClient.get("https://localhost:8080/service/api/v1/assets/cover")
                }
            } finally {
                http.close()
            }
        }
}
