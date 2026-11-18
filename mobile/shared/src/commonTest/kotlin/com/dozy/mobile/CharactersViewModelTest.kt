package com.dozy.mobile

import com.dozy.mobile.feature.character.domain.repository.CharacterRepository
import com.dozy.mobile.feature.character.domain.usecase.ObserveCharacters
import com.dozy.mobile.feature.character.domain.usecase.RefreshCharacters
import com.dozy.mobile.feature.character.presentation.list.CharactersAction
import com.dozy.mobile.feature.character.presentation.list.CharactersDestination
import com.dozy.mobile.feature.character.presentation.list.CharactersViewModel
import com.dozy.mobile.resources.*
import com.kjursa.foundation.presentation.navigation.Navigator
import com.kjursa.foundation.presentation.text.UiText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.*
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.fail

@OptIn(ExperimentalCoroutinesApi::class)
class CharactersViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeTest fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test fun loadingOfflineEmptyAndReactiveChanges() =
        runTest(dispatcher) {
            val repository = FakeCharacterRepository().apply { characters.value = emptyList() }
            val viewModel =
                CharactersViewModel(
                    Navigator(),
                    ObserveCharacters(repository),
                    RefreshCharacters(repository),
                )
            assertEquals(true, viewModel.uiState.value.loading)
            advanceUntilIdle()
            assertEquals(false, viewModel.uiState.value.loading)
            assertEquals(true, viewModel.uiState.value.offline)
            assertEquals(
                emptyList<String>(),
                viewModel.uiState.value.characters
                    .map { it.id },
            )
            repository.characters.value = listOf(character(), character("2", "Luna"))
            advanceUntilIdle()
            assertEquals(
                "Curious and brave",
                viewModel.uiState.value.characters
                    .first()
                    .description,
            )
            repository.characters.value = listOf(character("2", "Edited Luna"))
            advanceUntilIdle()
            assertEquals(
                listOf("Edited Luna"),
                viewModel.uiState.value.characters
                    .map { it.name },
            )
        }

    @Test fun refreshRetainsKnownRowsPreventsDuplicateRequestsAndCanRetry() =
        runTest(dispatcher) {
            val gate = CompletableDeferred<Boolean>()
            val repository = FakeCharacterRepository().apply { beforeRefresh = { gate.await() } }
            val viewModel =
                CharactersViewModel(
                    Navigator(),
                    ObserveCharacters(repository),
                    RefreshCharacters(repository),
                )
            advanceUntilIdle()
            assertEquals(true, viewModel.uiState.value.refreshing)
            repeat(3) { viewModel.dispatch(CharactersAction.Refresh) }
            assertEquals(1, repository.refreshes)
            gate.completeExceptionally(IllegalStateException("server unavailable"))
            advanceUntilIdle()
            assertEquals(true, viewModel.uiState.value.characterLoadError)
            assertEquals(3, viewModel.uiState.value.characters.size)
            repository.beforeRefresh = { true }
            viewModel.dispatch(CharactersAction.Refresh)
            advanceUntilIdle()
            assertEquals(false, viewModel.uiState.value.characterLoadError)
            assertEquals(false, viewModel.uiState.value.refreshing)
            assertEquals(2, repository.refreshes)
        }

    @Test fun localObservationFailureCanBeRetriedAndCancellationPropagates() =
        runTest(dispatcher) {
            val stored = FakeCharacterRepository()
            var failObservation = true
            val repository =
                object : CharacterRepository by stored {
                    override fun observe() =
                        flow {
                            if (failObservation) error("database unavailable")
                            emit(stored.characters.value)
                        }
                }
            val refresh =
                RefreshCharacters(repository)
            val viewModel = CharactersViewModel(Navigator(), ObserveCharacters(repository), refresh)
            advanceUntilIdle()
            assertEquals(false, viewModel.uiState.value.loading)
            assertEquals(true, viewModel.uiState.value.characterLoadError)
            failObservation = false
            viewModel.dispatch(CharactersAction.Refresh)
            advanceUntilIdle()
            assertEquals(false, viewModel.uiState.value.characterLoadError)
            assertEquals(3, viewModel.uiState.value.characters.size)
            stored.beforeRefresh = { throw CancellationException("cancelled") }
            try {
                refresh()
                fail("Expected cancellation")
            } catch (_: CancellationException) {
            }
        }

    @Test fun initialStateExposesPersistedCharacters() {
        val viewModel =
            CharactersViewModel(
                Navigator(),
                ObserveCharacters(FakeCharacterRepository()),
                RefreshCharacters(FakeCharacterRepository()),
            )
        dispatcher.scheduler.advanceUntilIdle()
        val state = viewModel.uiState.value
        assertEquals(UiText.Resource(Res.string.characters), state.title)
        assertEquals(listOf("Oliver", "Luna", "Captain Finn"), state.characters.map { it.name })
    }

    @Test fun selectingCharacterEmitsOpenCharacterEffect() =
        runTest(dispatcher) {
            val viewModel =
                CharactersViewModel(
                    Navigator(),
                    ObserveCharacters(FakeCharacterRepository()),
                    RefreshCharacters(FakeCharacterRepository()),
                )
            advanceUntilIdle()
            val character =
                viewModel.uiState.value.characters
                    .first()
            val effect = async { viewModel.navigator.commands.first() }
            viewModel.dispatch(CharactersAction.CharacterSelected(character))
            advanceUntilIdle()
            assertEquals(CharactersDestination.OpenCharacter(character), effect.await())
        }

    @Test fun addingCharacterEmitsNavigationEffect() =
        runTest(dispatcher) {
            val viewModel =
                CharactersViewModel(
                    Navigator(),
                    ObserveCharacters(FakeCharacterRepository()),
                    RefreshCharacters(FakeCharacterRepository()),
                )
            val effect = async { viewModel.navigator.commands.first() }
            viewModel.dispatch(CharactersAction.AddCharacterClicked)
            advanceUntilIdle()
            assertEquals(CharactersDestination.NavigateToAddCharacter, effect.await())
        }
}
