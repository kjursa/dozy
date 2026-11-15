package com.dozy.mobile

import com.dozy.mobile.core.config.ApiConfig
import com.dozy.mobile.feature.story.data.api.GenerationApi
import com.dozy.mobile.feature.story.data.api.GenerationStatusDto
import com.dozy.mobile.feature.story.data.api.KtorGenerationApi
import com.dozy.mobile.feature.story.data.api.StoryDto
import com.dozy.mobile.feature.story.data.api.StoryPageDto
import com.dozy.mobile.feature.story.data.api.StorySummaryDto
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
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

class StoryLibraryTest {
    private fun summary(
        id: String,
        time: Long = 100,
    ) = StorySummaryDto(id, "Story $id", "https://example.test/$id.png", 3, time)

    @Test fun completeTraversalReplacesRemoteCacheAndFailedDuplicateCycleOrCancellationRetainsIt() = runTest {
        val dao = FakeGenerationDao()
        var mode = "seed"
        val api = object : GenerationApi by ProgressApi() {
            override suspend fun stories(cursor: String?): StoryPageDto = when (mode) {
                "seed" -> StoryPageDto(listOf(summary("old")))
                "empty" -> StoryPageDto(emptyList())
                else -> if (cursor == null) StoryPageDto(listOf(summary("new")), "next") else when (mode) {
                    "duplicate" -> StoryPageDto(listOf(summary("new")))
                    "cycle" -> StoryPageDto(listOf(summary("another")), "next")
                    "cancel" -> throw CancellationException("cancel second page")
                    else -> error("failed second page")
                }
            }
        }
        val repository = testGenerationRepository(dao, api)
        assertTrue(repository.refreshStories())
        val before = dao.rows.value
        for (failure in listOf("duplicate", "cycle", "failure", "cancel")) {
            mode = failure
            try { repository.refreshStories(); fail("Must reject traversal") } catch (_: IllegalArgumentException) {
            } catch (_: IllegalStateException) {
            } catch (_: CancellationException) {
            }
            assertEquals(before, dao.rows.value)
        }
        mode = "empty"
        assertTrue(repository.refreshStories())
        assertTrue(repository.summaries().first().isEmpty())
    }

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
            val repo = testGenerationRepository(dao, api)
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
            assertEquals(summaries, testGenerationRepository(dao, api).summaries().first())
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
                    override suspend fun stories(cursor: String?): StoryPageDto = throw CancellationException("cancel")
                }
            try {
                RefreshStories(testGenerationRepository(FakeGenerationDao(), api))()
                fail("Cancellation must propagate")
            } catch (_: CancellationException) {
            }
        }
}
