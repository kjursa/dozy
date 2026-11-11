package com.dozy.mobile

import androidx.lifecycle.SavedStateHandle
import com.dozy.mobile.core.config.ApiConfig
import com.dozy.mobile.feature.character.domain.repository.CharacterRepository
import com.dozy.mobile.feature.character.domain.usecase.ObserveCharacters
import com.dozy.mobile.feature.place.domain.repository.PlaceRepository
import com.dozy.mobile.feature.place.domain.usecase.SelectPlace
import com.dozy.mobile.feature.story.data.database.GenerationDao
import com.dozy.mobile.feature.story.di.storyModule
import com.dozy.mobile.feature.story.domain.model.GenerationJob
import com.dozy.mobile.feature.story.domain.usecase.GenerationKeyGenerator
import com.dozy.mobile.feature.story.presentation.create.*
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test
import org.koin.core.parameter.parametersOf
import org.koin.dsl.koinApplication
import org.koin.dsl.module

@OptIn(ExperimentalCoroutinesApi::class)
class StoryModuleTest {
    @Test fun graphInjectsSubmissionAndRestoresPreselectionWithoutNetwork() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val client = HttpClient(MockEngine { error("Disabled configuration must not send HTTP") })
            val platform =
                module {
                    single { ApiConfig(null) }
                    single { client }
                    single<GenerationDao> { FakeGenerationDao() }
                    single<CharacterRepository> { FakeCharacterRepository() }
                    single<PlaceRepository> { FakePlaceRepository() }
                    factory { ObserveCharacters(get()) }
                    factory { SelectPlace(get()) }
                    single<GenerationKeyGenerator> { GenerationKeyGenerator { GenerationJob.RequestKey("injected") } }
                }
            val app = koinApplication { modules(storyModule, platform) }
            try {
                val vm =
                    app.koin.get<CreateStoryViewModel> {
                        parametersOf(
                            SavedStateHandle(mapOf("place" to "custom", "cast" to "[\"1\"]")),
                        )
                    }
                advanceUntilIdle()
                assertEquals(listOf("1"), vm.uiState.value.selectedHeroes)
                assertEquals("Garden", vm.uiState.value.placeName)
                vm.dispatch(CreateStoryAction.Automatic(true))
                vm.dispatch(CreateStoryAction.Submit)
                advanceUntilIdle()
                assertEquals("injected", vm.uiState.value.requestKey)
                assertNotNull(vm.uiState.value.submissionError)
                assertNull(vm.uiState.value.acceptedJobId)
            } finally {
                app.close()
                client.close()
                Dispatchers.resetMain()
            }
        }
}
