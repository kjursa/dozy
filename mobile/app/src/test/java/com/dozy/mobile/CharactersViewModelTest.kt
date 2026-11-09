package com.dozy.mobile

import com.dozy.mobile.domain.character.ObserveCharacters
import com.dozy.mobile.presentation.characters.CharactersAction
import com.dozy.mobile.presentation.characters.CharactersEffect
import com.dozy.mobile.presentation.characters.CharactersViewModel
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
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CharactersViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test fun initialStateExposesPersistedCharacters() {
        val viewModel =
            CharactersViewModel(
                com.dozy.mobile.domain.character
                    .ObserveCharacters(FakeCharacterRepository()),
            )
        dispatcher.scheduler.advanceUntilIdle()
        val state = viewModel.uiState.value
        assertEquals("Characters", state.title)
        assertEquals(listOf("Oliver", "Luna", "Captain Finn"), state.characters.map { it.name })
    }

    @Test fun selectingCharacterEmitsOpenCharacterEffect() =
        runTest(dispatcher) {
            val viewModel =
                CharactersViewModel(
                    com.dozy.mobile.domain.character
                        .ObserveCharacters(FakeCharacterRepository()),
                )
            advanceUntilIdle()
            val character =
                viewModel.uiState.value.characters
                    .first()
            val effect = async { viewModel.effects.first() }
            viewModel.dispatch(CharactersAction.CharacterSelected(character))
            advanceUntilIdle()
            assertEquals(CharactersEffect.OpenCharacter(character), effect.await())
        }

    @Test fun addingCharacterEmitsNavigationEffect() =
        runTest(dispatcher) {
            val viewModel =
                CharactersViewModel(
                    com.dozy.mobile.domain.character
                        .ObserveCharacters(FakeCharacterRepository()),
                )
            val effect = async { viewModel.effects.first() }
            viewModel.dispatch(CharactersAction.AddCharacterClicked)
            advanceUntilIdle()
            assertEquals(CharactersEffect.NavigateToAddCharacter, effect.await())
        }
}
