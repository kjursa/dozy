package com.dozy.mobile

import com.dozy.mobile.core.config.ApiConfig
import com.dozy.mobile.feature.story.data.*
import com.dozy.mobile.feature.story.data.api.*
import com.dozy.mobile.feature.story.domain.model.*
import com.dozy.mobile.feature.story.domain.usecase.*
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class StoryLibraryTest {
    private fun summary(
        id: String,
        time: Long = 100,
    ) = StorySummaryDto(id, "Story $id", "https://example.test/$id.png", 3, time)

    @Test fun mergesAllPagesWithCompletedJobsWithoutDuplicatesAndKeepsSavedText() =
        runTest {
            val progress = ProgressApi()
            val cursors = mutableListOf<String?>()
            var fail = false
            val api =
                object : GenerationApi by progress {
                    override suspend fun stories(cursor: String?): StoryPageDto {
                        cursors += cursor
                        if (fail && cursor != null) error("Network unavailable")
                        return if (cursor == null) {
                            StoryPageDto(listOf(summary("story")), "page 2")
                        } else {
                            StoryPageDto(listOf(summary("new", 200), summary("older", 50)))
                        }
                    }
                }
            val dao = FakeGenerationDao()
            val repo = DefaultGenerationRepository(dao, api)
            assertTrue(ObserveStoryLibrary(repo)().first().isEmpty())
            repo.start(GenerationJob.RequestKey("key"), progress.configuration)
            progress.remote = GenerationStatusDto("job", "READY", "story")
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
            assertEquals(listOf(null, "page 2"), cursors)
            val summaries = repo.summaries().first()
            assertEquals(listOf("new", "story", "older"), summaries.map { it.id.value })
            assertEquals(StorySummary.ReadingMinutes(3), summaries[1].readingMinutes)
            assertEquals("Once upon a time", repo.readStory(Story.Id("story"))?.text)
            fail = true
            assertEquals(RefreshStories.Result.Failure, RefreshStories(repo)())
            assertEquals(summaries, DefaultGenerationRepository(dao, api).summaries().first())
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
                        assertEquals("cursor /+", request.url.parameters["cursor"])
                        respond(
                            """{"stories":[{"id":"a","title":"Title","coverUrl":"https://example.test/a","readingMinutes":2,"createdAt":123}],"nextCursor":"next"}""",
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
                        .stories("cursor /+")
                        ?.stories
                        ?.single()
                        ?.id,
                )
                assertEquals("next", api.stories("cursor /+")?.nextCursor)
                assertNull(KtorGenerationApi(client, ApiConfig(null)).stories(null))
            } finally {
                client.close()
            }
        }

    @Test fun failedMalformedOrCyclicPaginationCannotEraseCacheAndCancellationPropagates() =
        runTest {
            for ((status, body) in listOf(
                HttpStatusCode.ServiceUnavailable to "{}",
                HttpStatusCode.OK to """{"stories":[{"id":"a"}]}""",
                HttpStatusCode.OK to """{"stories":[],"nextCursor":"loop"}""",
            )) {
                val client =
                    HttpClient(
                        MockEngine {
                            respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
                        },
                    ) { install(ContentNegotiation) { json() } }
                try {
                    val dao = FakeGenerationDao()
                    val local = DefaultGenerationRepository(dao, ProgressApi())
                    local.readStory(Story.Id("story"))
                    val repo = DefaultGenerationRepository(dao, KtorGenerationApi(client, ApiConfig("https://example.test")))
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
                    override suspend fun stories(cursor: String?): StoryPageDto = throw CancellationException("cancel")
                }
            try {
                RefreshStories(DefaultGenerationRepository(FakeGenerationDao(), api))()
                fail("Cancellation must propagate")
            } catch (_: CancellationException) {
            }
        }
}
