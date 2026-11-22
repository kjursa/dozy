package com.dozy.mobile

import com.dozy.api.contracts.v1.story.StoryDto
import com.dozy.api.contracts.v1.story.StorySummaryDto
import com.dozy.api.contracts.v1.story.StorySyncDto
import com.dozy.mobile.core.config.ApiConfig
import com.dozy.mobile.feature.story.data.api.GenerationApi
import com.dozy.mobile.feature.story.data.api.KtorGenerationApi
import com.dozy.mobile.feature.story.data.testGenerationRepository
import com.dozy.mobile.feature.story.data.toDomain
import com.dozy.mobile.feature.story.data.toDto
import com.dozy.mobile.feature.story.data.toSummaryDto
import com.dozy.mobile.feature.story.domain.model.GenerationJob
import com.dozy.mobile.feature.story.domain.model.Story
import com.dozy.mobile.feature.story.domain.model.StorySummary
import com.dozy.mobile.feature.story.domain.usecase.ObserveStoryLibrary
import com.dozy.mobile.feature.story.domain.usecase.RefreshStories
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail

class StoryLibraryTest {
    private fun summary(
        id: String,
        time: Long = 100,
    ) = StorySummaryDto(id, "Story $id", "https://example.test/$id.png", 3, time)

    @Test fun syncAppliesDeltasFromPersistedRevisionAndKeepsDownloadedText() =
        runTest {
            val progress = ProgressApi()
            val revisions = mutableListOf<Long>()
            var unavailable = false
            val api =
                object : GenerationApi by progress {
                    override suspend fun sync(after: Long): StorySyncDto {
                        revisions += after
                        check(!unavailable)
                        return when (after) {
                            0L -> StorySyncDto(listOf(summary("story"), summary("older", 50)), emptyList(), 4)
                            4L -> StorySyncDto(listOf(summary("new", 200)), listOf("older"), 9)
                            9L -> StorySyncDto(emptyList(), listOf("story"), 12)
                            else -> StorySyncDto(emptyList(), emptyList(), after)
                        }
                    }
                }
            val dao = FakeGenerationDao()
            val repo = testGenerationRepository(dao, api)
            assertTrue(ObserveStoryLibrary(repo)().first().isEmpty())
            repo.start(GenerationJob.RequestKey("key"), progress.configuration)
            progress.remote = progress.ready()
            repo.refresh(GenerationJob.Id("job"))
            assertEquals(
                "story",
                repo
                    .summaries()
                    .first()
                    .single()
                    .id.value,
            )
            assertEquals(RefreshStories.Result.Refreshed, RefreshStories(repo)())
            assertEquals(
                StorySummary.ReadingMinutes(3),
                repo
                    .summaries()
                    .first()
                    .first()
                    .readingMinutes,
            )

            val restored = testGenerationRepository(dao, api)
            unavailable = true
            assertEquals(RefreshStories.Result.Failure, RefreshStories(restored)())
            assertEquals(
                setOf("story", "older"),
                restored
                    .summaries()
                    .first()
                    .map { it.id.value }
                    .toSet(),
            )
            unavailable = false
            assertTrue(restored.refreshStories())
            assertEquals(
                setOf("story", "new"),
                restored
                    .summaries()
                    .first()
                    .map { it.id.value }
                    .toSet(),
            )
            assertEquals("Once upon a time", restored.readStory(Story.Id("story"))?.text)
            assertEquals(0, progress.storyReads)
            assertTrue(restored.refreshStories())
            assertEquals(
                "new",
                restored
                    .summaries()
                    .first()
                    .single()
                    .id.value,
            )
            assertNull(dao.findStory("story"))
            assertEquals(listOf(0L, 4L, 4L, 9L), revisions)
        }

    @Test fun summaryValidationAndTextReadingTimeMatchReaderEstimate() {
        val dto = ProgressApi().run { StoryDto("story", "Title", "word ".repeat(151), "https://example.test/cover", configuration.toDto()) }
        assertEquals(
            2,
            dto
                .toSummaryDto()
                .toDomain()
                .readingMinutes.value,
        )
        for (bad in listOf(
            summary("../bad"),
            summary("ok").copy(readingMinutes = 0),
            summary("ok").copy(createdAt = -1),
            summary("ok").copy(title = ""),
            summary("ok").copy(coverUrl = "http://bad"),
        )) {
            assertFailsWith<IllegalArgumentException> { bad.toDomain() }
        }
    }

    @Test fun httpUsesGetPaginationAndDisabledModeNeverRequests() =
        runTest {
            val client =
                HttpClient(
                    MockEngine { request ->
                        assertEquals(HttpMethod.Get, request.method)
                        assertEquals("/service/api/v1/stories", request.url.encodedPath)
                        assertEquals("100", request.url.parameters["limit"])
                        assertEquals("b", request.url.parameters["cursor"])
                        respond(
                            """{"stories":[{"id":"a","title":"Title","coverUrl":"https://example.test/a","readingMinutes":2,"createdAt":123}],"nextCursor":"a"}""",
                            HttpStatusCode.OK,
                            headersOf(HttpHeaders.ContentType, "application/json"),
                        )
                    },
                ) { install(ContentNegotiation) { json() } }
            try {
                val api = KtorGenerationApi(client, ApiConfig("https://example.test/service/"))
                assertEquals(
                    "a",
                    api
                        .stories("b")
                        ?.stories
                        ?.single()
                        ?.id,
                )
                assertEquals("a", api.stories("b")?.nextCursor)
                assertNull(KtorGenerationApi(client, ApiConfig(null)).stories(null))
            } finally {
                client.close()
            }
        }

    @Test fun failedOrMalformedSyncCannotEraseCacheAndCancellationPropagates() =
        runTest {
            for ((status, body) in listOf(
                HttpStatusCode.ServiceUnavailable to "{}",
                HttpStatusCode.OK to """{"upserts":[{"id":"a"}],"deletedIds":[],"nextRevision":1}""",
                HttpStatusCode.OK to """{"upserts":[],"deletedIds":["story"],"nextRevision":0}""",
            )) {
                val client =
                    HttpClient(
                        MockEngine {
                            respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
                        },
                    ) { install(ContentNegotiation) { json() } }
                try {
                    val dao = FakeGenerationDao()
                    val local = testGenerationRepository(dao, ProgressApi())
                    local.readStory(Story.Id("story"))
                    val repo = testGenerationRepository(dao, KtorGenerationApi(client, ApiConfig("https://example.test")))
                    assertEquals(RefreshStories.Result.Failure, RefreshStories(repo)())
                    assertEquals(
                        "story",
                        repo
                            .summaries()
                            .first()
                            .single()
                            .id.value,
                    )
                } finally {
                    client.close()
                }
            }
            val api =
                object : GenerationApi by ProgressApi() {
                    override suspend fun sync(after: Long): StorySyncDto = throw CancellationException("cancel")
                }
            try {
                RefreshStories(testGenerationRepository(FakeGenerationDao(), api))()
                fail("Cancellation must propagate")
            } catch (_: CancellationException) {
            }
        }
}
