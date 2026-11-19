package com.dozy.mobile

import androidx.lifecycle.SavedStateHandle
import com.dozy.api.contracts.v1.story.GenerationAcceptedDto
import com.dozy.api.contracts.v1.story.GenerationRequestDto
import com.dozy.api.contracts.v1.story.GenerationStatusDto
import com.dozy.api.contracts.v1.story.StoryDto
import com.dozy.mobile.feature.character.domain.usecase.ObserveCharacters
import com.dozy.mobile.feature.place.domain.usecase.SelectPlace
import com.dozy.mobile.feature.story.data.api.GenerationApi
import com.dozy.mobile.feature.story.data.testGenerationRepository
import com.dozy.mobile.feature.story.domain.model.GenerationInput
import com.dozy.mobile.feature.story.domain.model.GenerationJob
import com.dozy.mobile.feature.story.domain.usecase.GenerationKeyGenerator
import com.dozy.mobile.feature.story.domain.usecase.StartGeneration
import com.dozy.mobile.feature.story.presentation.create.CreateStoryAction
import com.dozy.mobile.feature.story.presentation.create.CreateStoryDestination
import com.dozy.mobile.feature.story.presentation.create.CreateStoryUiState
import com.dozy.mobile.feature.story.presentation.create.CreateStoryViewModel
import com.dozy.mobile.navigation.AppNavigator
import com.kjursa.foundation.compose.mvi.SavedStatePersistence
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GenerationRestorationTest {
    private fun input(automatic: Boolean = false) =
        (GenerationInput.from(listOf("1", "1"), automatic, 0, "custom", "  A star  ", "Home") as GenerationInput.Validation.Valid).input

    private val key = GenerationJob.RequestKey("stable-key")

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
                        testGenerationRepository(
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
                        AppNavigator(),
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
                assertEquals(CreateStoryDestination.Accepted("job"), restored.navigator.commands.first())
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
