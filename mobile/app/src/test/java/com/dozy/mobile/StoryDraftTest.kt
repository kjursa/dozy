package com.dozy.mobile

import androidx.lifecycle.SavedStateHandle
import com.dozy.mobile.feature.character.domain.usecase.ObserveCharacters
import com.dozy.mobile.feature.place.domain.usecase.SelectPlace
import com.dozy.mobile.feature.story.presentation.create.CreateStoryAction
import com.dozy.mobile.feature.story.presentation.create.CreateStoryEffect
import com.dozy.mobile.feature.story.presentation.create.CreateStoryUiState
import com.dozy.mobile.feature.story.presentation.create.CreateStoryViewModel
import com.kjursa.foundation.compose.mvi.SavedStatePersistence
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StoryDraftTest {
    private val dispatcher = StandardTestDispatcher()

    @Before fun setup() {
        Dispatchers.setMain(dispatcher)
    }

    @After fun teardown() {
        Dispatchers.resetMain()
    }

    private fun vm(
        handle: SavedStateHandle,
        initial: String? = null,
    ) = CreateStoryViewModel(
        SavedStatePersistence(handle, CreateStoryUiState.serializer()),
        initial,
        SelectPlace(FakePlaceRepository()),
        ObserveCharacters(FakeCharacterRepository()),
        testStartGeneration(),
        com.dozy.mobile.feature.story.domain.usecase.GenerationKeyGenerator {
            com.dozy.mobile.feature.story.domain.model.GenerationJob
                .RequestKey("key")
        },
    )

    @Test fun selectingOnlyChangesSettingAndRestoresAllOtherInputAfterRecreation() =
        runTest(dispatcher) {
            val handle = SavedStateHandle()
            val draft = vm(handle)
            advanceUntilIdle()
            draft.dispatch(CreateStoryAction.Request("Find a lost kite"))
            draft.dispatch(CreateStoryAction.Ending("Everyone gets home"))
            draft.dispatch(CreateStoryAction.Hero("luna"))
            draft.dispatch(CreateStoryAction.Length(1))
            val before = draft.uiState.value
            val open = async { draft.effects.first() }
            draft.dispatch(CreateStoryAction.ChoosePlace)
            draft.dispatch(CreateStoryAction.ChoosePlace)
            advanceUntilIdle()
            assertEquals(CreateStoryEffect.OpenPlaces, open.await())
            // Back from Places has no draft action and leaves every input intact.
            assertEquals(before, draft.uiState.value)
            draft.dispatch(CreateStoryAction.Setting("custom"))
            advanceUntilIdle()
            assertEquals(before.copy(placeId = "custom", randomSetting = false, placeName = "Garden"), draft.uiState.value)
            val restored =
                vm(SavedStateHandle(mapOf(SavedStatePersistence.DEFAULT_KEY to handle.get<String>(SavedStatePersistence.DEFAULT_KEY))))
            advanceUntilIdle()
            assertEquals(draft.uiState.value, restored.uiState.value)
            restored.dispatch(CreateStoryAction.Setting(null))
            assertEquals(before, restored.uiState.value)
            assertFalse(restored.uiState.value.automaticPlot)
        }

    @Test fun homeSelectionStartsNewDraftWithSettingAndDefaultFields() =
        runTest(dispatcher) {
            val draft = vm(SavedStateHandle(), "custom")
            advanceUntilIdle()
            assertEquals("custom", draft.uiState.value.placeId)
            assertEquals("Garden", draft.uiState.value.placeName)
            assertEquals("", draft.uiState.value.specialRequest)
            assertFalse(draft.uiState.value.automaticPlot)
            assertEquals(emptyList<String>(), draft.uiState.value.selectedHeroes)
        }
}
