package com.dozy.mobile

import androidx.lifecycle.SavedStateHandle
import com.dozy.mobile.feature.character.domain.usecase.ObserveCharacters
import com.dozy.mobile.feature.place.domain.usecase.SelectPlace
import com.dozy.mobile.feature.story.data.*
import com.dozy.mobile.feature.story.data.api.*
import com.dozy.mobile.feature.story.domain.model.*
import com.dozy.mobile.feature.story.domain.usecase.*
import com.dozy.mobile.feature.story.presentation.create.*
import com.kjursa.foundation.compose.mvi.SavedStatePersistence
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import kotlinx.serialization.json.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class GenerationTest {
    private fun input(automatic: Boolean = false) =
        (GenerationInput.from(listOf("1", "1"), automatic, 0, "custom", "  A star  ", "Home") as GenerationInput.Validation.Valid).input

    private val key = GenerationJob.RequestKey("stable-key")

    @Test fun loadingFailureCanRetryAndEmptyCastCannotSubmit() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            try {
                val characters = FakeCharacterRepository()
                var failObservation = true
                val source =
                    object : com.dozy.mobile.feature.character.domain.repository.CharacterRepository by characters {
                        override fun observe() =
                            kotlinx.coroutines.flow.flow {
                                if (failObservation) error("Read unavailable")
                                emit(emptyList<com.dozy.mobile.feature.character.domain.model.Character>())
                            }
                    }
                val vm =
                    CreateStoryViewModel(
                        SavedStatePersistence(SavedStateHandle(), CreateStoryUiState.serializer()),
                        null,
                        SelectPlace(FakePlaceRepository()),
                        ObserveCharacters(source),
                        testStartGeneration(),
                        GenerationKeyGenerator { error("Invalid input must not allocate a key") },
                    )
                assertTrue(vm.uiState.value.characterLoading)
                advanceUntilIdle()
                assertTrue(vm.uiState.value.characterLoadError)
                failObservation = false
                vm.dispatch(CreateStoryAction.ReloadCharacters)
                advanceUntilIdle()
                assertFalse(vm.uiState.value.characterLoadError)
                assertFalse(vm.uiState.value.characterLoading)
                assertTrue(
                    vm.uiState.value.heroes
                        .isEmpty(),
                )
                vm.dispatch(CreateStoryAction.Submit)
                assertNotNull(vm.uiState.value.castError)
            } finally {
                Dispatchers.resetMain()
            }
        }
}
