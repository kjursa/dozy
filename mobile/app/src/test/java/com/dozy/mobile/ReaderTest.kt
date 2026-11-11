package com.dozy.mobile

import androidx.lifecycle.SavedStateHandle
import com.dozy.mobile.core.config.ApiConfig
import com.dozy.mobile.feature.story.data.*
import com.dozy.mobile.feature.story.data.api.*
import com.dozy.mobile.feature.story.domain.model.*
import com.dozy.mobile.feature.story.domain.usecase.ReadStory
import com.dozy.mobile.feature.story.presentation.reader.*
import com.kjursa.foundation.compose.mvi.SavedStatePersistence
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReaderTest {
    private val id = Story.Id("story")

    @Test fun remoteMappingIsDurableAndLocalReadNeverCallsBackend() =
        runTest {
            val dao = FakeGenerationDao()
            val api = ProgressApi()
            val reader = ReadStory(DefaultGenerationRepository(dao, api))
            val first = reader(id) as ReadStory.Result.Found
            assertEquals("Once upon a time", first.story.text)
            assertEquals(api.configuration.cast, first.story.configuration.cast)
            assertEquals("https://example.test/cover.png", first.story.coverUrl)
            assertEquals(1, first.story.readingMinutes())
            api.storyUnavailable = true
            val restored = ReadStory(DefaultGenerationRepository(dao, api))(id)
            assertEquals(first, restored)
            assertEquals(1, api.storyReads)
            assertEquals(first.story, DefaultGenerationRepository(dao, api).stories().first().single())
        }

    @Test fun httpUsesExactIdHandles404FailuresAndDisabledMode() =
        runTest {
            var status = HttpStatusCode.OK
            var calls = 0
            var body = Json.encodeToString(ProgressApi().story("story"))
            val client =
                HttpClient(
                    MockEngine { request ->
                        calls++
                        assertEquals("/stories/story", request.url.encodedPath)
                        assertEquals(HttpMethod.Get, request.method)
                        respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
                    },
                ) { install(ContentNegotiation) { json() } }
            try {
                suspend fun read(config: ApiConfig = ApiConfig("https://example.test")) =
                    ReadStory(DefaultGenerationRepository(FakeGenerationDao(), KtorGenerationApi(client, config)))(id)
                assertTrue(read() is ReadStory.Result.Found)
                status = HttpStatusCode.NotFound
                assertEquals(ReadStory.Result.Missing, read())
                status = HttpStatusCode.ServiceUnavailable
                assertEquals(ReadStory.Result.Unavailable, read())
                status = HttpStatusCode.OK
                body = body.replace("\"id\":\"story\"", "\"id\":\"other\"")
                assertEquals(ReadStory.Result.Unavailable, read())
                val previous = calls
                assertEquals(ReadStory.Result.Unavailable, read(ApiConfig(null)))
                assertEquals(previous, calls)
            } finally {
                client.close()
            }
        }

    @Test fun freshAdventureSelectsOnlyExistingCastAndHasNoPreviousPlot() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            try {
                val vm =
                    com.dozy.mobile.feature.story.presentation.create.CreateStoryViewModel(
                        SavedStatePersistence(
                            SavedStateHandle(),
                            com.dozy.mobile.feature.story.presentation.create.CreateStoryUiState
                                .serializer(),
                        ),
                        null,
                        com.dozy.mobile.feature.place.domain.usecase
                            .SelectPlace(FakePlaceRepository()),
                        com.dozy.mobile.feature.character.domain.usecase
                            .ObserveCharacters(FakeCharacterRepository()),
                        testStartGeneration(),
                        com.dozy.mobile.feature.story.domain.usecase
                            .GenerationKeyGenerator { GenerationJob.RequestKey("key") },
                        listOf("1", "deleted"),
                    )
                advanceUntilIdle()
                assertEquals(listOf("1"), vm.uiState.value.selectedHeroes)
                assertEquals("", vm.uiState.value.specialRequest)
                assertEquals("", vm.uiState.value.ending)
                assertNull(vm.uiState.value.placeId)
            } finally {
                Dispatchers.resetMain()
            }
        }

    @Test fun readCancellationPropagates() =
        runTest {
            val api =
                object : GenerationApi by ProgressApi() {
                    override suspend fun story(id: String): StoryDto? = throw CancellationException("Cancelled")
                }
            try {
                ReadStory(DefaultGenerationRepository(FakeGenerationDao(), api))(id)
                fail("Cancellation must propagate")
            } catch (_: CancellationException) {
            }
        }

    @Test fun readerRetriesKeepsContentAcrossOverlaysRestoresSettingsAndEmitsOnlyCast() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            try {
                val progress = ProgressApi().apply { storyUnavailable = true }
                val api =
                    object : GenerationApi by progress {
                        override suspend fun story(id: String): StoryDto? {
                            val dto = progress.story(id)
                            if (progress.storyUnavailable) throw StoryNotFoundException()
                            return dto
                        }
                    }
                val read = ReadStory(DefaultGenerationRepository(FakeGenerationDao(), api))
                val handle = SavedStateHandle()
                val persistence = SavedStatePersistence(handle, ReaderUiState.serializer())
                val vm = ReaderViewModel(id, read, persistence)
                assertTrue(vm.uiState.value.loading)
                vm.dispatch(ReaderAction.Retry)
                advanceUntilIdle()
                assertNull(vm.uiState.value.story)
                assertEquals(1, progress.storyReads)
                progress.storyUnavailable = false
                vm.dispatch(ReaderAction.Retry)
                vm.dispatch(ReaderAction.Retry)
                advanceUntilIdle()
                assertEquals(2, progress.storyReads)
                val story = vm.uiState.value.story
                vm.dispatch(ReaderAction.Settings)
                vm.dispatch(ReaderAction.Size(ReaderUiState.TextSize.LARGE))
                vm.dispatch(ReaderAction.Evening(true))
                vm.dispatch(ReaderAction.Back)
                assertNull(vm.uiState.value.overlay)
                vm.dispatch(ReaderAction.Cover)
                vm.dispatch(ReaderAction.Dismiss)
                assertEquals(story, vm.uiState.value.story)
                val restored = ReaderViewModel(id, read, persistence)
                advanceUntilIdle()
                assertTrue(restored.uiState.value.evening)
                assertEquals(ReaderUiState.TextSize.LARGE, restored.uiState.value.textSize)
                assertNull(restored.uiState.value.overlay)
                vm.dispatch(ReaderAction.NewAdventure)
                vm.dispatch(ReaderAction.NewAdventure)
                assertEquals(ReaderEffect.NewAdventure(progress.configuration.cast.map { it.id }), vm.effects.first())
                vm.resumed()
                vm.dispatch(ReaderAction.Library)
                assertEquals(ReaderEffect.Library, vm.effects.first())
            } finally {
                Dispatchers.resetMain()
            }
        }
}
