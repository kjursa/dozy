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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test

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

    @Test fun viewModelValidatesBlocksDuplicatesAndRestoresRetryKeyAndDraft() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            try {
                val dao = FakeGenerationDao()
                val characters = FakeCharacterRepository()
                val places = FakePlaceRepository()
                var calls = 0
                var keys = 0
                val gate = CompletableDeferred<Unit>()
                val start =
                    StartGeneration(
                        DefaultGenerationRepository(
                            dao,
                            object : GenerationApi {
                                override suspend fun status(id: String): GenerationStatusDto? = null

                                override suspend fun story(id: String): StoryDto? = null

                                override suspend fun retry(
                                    id: String,
                                    key: String,
                                ): GenerationAcceptedDto? = null

                                override suspend fun start(
                                    key: String,
                                    request: GenerationRequestDto,
                                ): GenerationAcceptedDto {
                                    calls++
                                    gate.await()
                                    if (calls == 1) error("Uncertain")
                                    return GenerationAcceptedDto("job", "QUEUED")
                                }
                            },
                        ),
                        characters,
                        places,
                    )

                fun vm(handle: SavedStateHandle) =
                    CreateStoryViewModel(
                        SavedStatePersistence(handle, CreateStoryUiState.serializer()),
                        null,
                        SelectPlace(places),
                        ObserveCharacters(characters),
                        start,
                        GenerationKeyGenerator {
                            keys++
                            key
                        },
                    )
                val handle = SavedStateHandle()
                val draft = vm(handle)
                advanceUntilIdle()
                draft.dispatch(CreateStoryAction.Submit)
                assertNotNull(draft.uiState.value.castError)
                assertEquals(0, calls)
                draft.dispatch(CreateStoryAction.Hero("1"))
                draft.dispatch(CreateStoryAction.Request("Keep the moon"))
                draft.dispatch(CreateStoryAction.Submit)
                draft.dispatch(CreateStoryAction.Submit)
                runCurrent()
                assertTrue(draft.uiState.value.submitting)
                assertEquals(1, calls)
                gate.complete(Unit)
                advanceUntilIdle()
                assertNotNull(draft.uiState.value.submissionError)
                assertEquals("Keep the moon", draft.uiState.value.specialRequest)
                val restored =
                    vm(
                        SavedStateHandle(
                            mapOf(
                                SavedStatePersistence.DEFAULT_KEY to handle.get<String>(SavedStatePersistence.DEFAULT_KEY),
                            ),
                        ),
                    )
                assertFalse(restored.uiState.value.submitting)
                assertNull(restored.uiState.value.submissionError)
                restored.dispatch(CreateStoryAction.Submit)
                advanceUntilIdle()
                assertEquals(CreateStoryEffect.Accepted("job"), restored.effects.first())
                assertEquals(1, keys)
                restored.dispatch(CreateStoryAction.Submit)
                advanceUntilIdle()
                assertEquals(2, calls)
                assertEquals("job", restored.uiState.value.acceptedJobId)
            } finally {
                Dispatchers.resetMain()
            }
        }
}
