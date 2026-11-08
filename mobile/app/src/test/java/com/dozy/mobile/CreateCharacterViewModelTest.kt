package com.dozy.mobile

import androidx.lifecycle.SavedStateHandle
import com.dozy.mobile.domain.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CreateCharacterViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val repository = FakeCharacterRepository()
    private val validator = CharacterValidator()

    @Before fun setup() {
        Dispatchers.setMain(dispatcher)
    }

    @After fun teardown() {
        Dispatchers.resetMain()
    }

    @Test fun editsRestoreAndShowFieldErrors() =
        runTest(dispatcher) {
            val handle = SavedStateHandle()
            val vm = viewModel(handle)
            vm.dispatch(CreateCharacterAction.NameChanged(""))
            vm.dispatch(CreateCharacterAction.PersonalityChanged(""))
            vm.dispatch(CreateCharacterAction.AppearanceChanged(""))
            vm.dispatch(CreateCharacterAction.TypeSelected("Unknown"))
            vm.dispatch(CreateCharacterAction.SaveClicked)
            advanceUntilIdle()
            assertEquals(CharacterField.entries.toSet(), vm.uiState.value.errors.keys)
            assertEquals(0, repository.saves)
            vm.dispatch(CreateCharacterAction.NameChanged("Finn"))
            vm.dispatch(CreateCharacterAction.PersonalityChanged("Kind"))
            vm.dispatch(CreateCharacterAction.AppearanceChanged("Green eyes"))
            vm.dispatch(CreateCharacterAction.TypeSelected("Fantasy"))
            assertTrue(
                vm.uiState.value.errors
                    .isEmpty(),
            )
            assertEquals("Finn", viewModel(handle).uiState.value.name)
            assertEquals("Fantasy", viewModel(handle).uiState.value.selectedType)
            assertEquals("Kind", viewModel(handle).uiState.value.personality)
            assertEquals("Green eyes", viewModel(handle).uiState.value.appearance)
        }

    @Test fun savingSuccessAndDoneAvoidDuplicateSaves() =
        runTest(dispatcher) {
            val gate = CompletableDeferred<Unit>()
            repository.beforeSave = { gate.await() }
            val vm = viewModel()
            vm.dispatch(CreateCharacterAction.SaveClicked)
            assertEquals(SaveStatus.SAVING, vm.uiState.value.saveStatus)
            vm.dispatch(CreateCharacterAction.SaveClicked)
            vm.dispatch(CreateCharacterAction.NameChanged("Ignored during save"))
            runCurrent()
            assertEquals(1, repository.saves)
            gate.complete(Unit)
            advanceUntilIdle()
            assertEquals(SaveStatus.SUCCESS, vm.uiState.value.saveStatus)
            assertEquals(CharacterSync.PENDING, vm.uiState.value.sync)
            val effect = async { vm.effects.first() }
            vm.dispatch(CreateCharacterAction.SaveClicked)
            advanceUntilIdle()
            assertEquals(CreateCharacterEffect.Saved, effect.await())
            assertEquals(1, repository.saves)
        }

    @Test fun failedPersistenceCanBeRetried() =
        runTest(dispatcher) {
            repository.failure = IllegalStateException("disk full")
            val vm = viewModel()
            vm.dispatch(CreateCharacterAction.SaveClicked)
            advanceUntilIdle()
            assertEquals(SaveStatus.FAILURE, vm.uiState.value.saveStatus)
            repository.failure = null
            vm.dispatch(CreateCharacterAction.SaveClicked)
            advanceUntilIdle()
            assertEquals(SaveStatus.SUCCESS, vm.uiState.value.saveStatus)
        }

    @Test fun savedCharacterUpdatesBothViewsAndSurvivesViewModelRecreation() =
        runTest(dispatcher) {
            val observe = ObserveCharacters(repository)
            val home = HomeViewModel(observe)
            val characters = CharactersViewModel(observe)
            val vm = viewModel()
            vm.dispatch(CreateCharacterAction.NameChanged("New hero"))
            vm.dispatch(CreateCharacterAction.SaveClicked)
            advanceUntilIdle()
            assertEquals(
                "New hero",
                home.uiState.value.characters
                    .last()
                    .name,
            )
            assertEquals(
                "New hero",
                characters.uiState.value.characters
                    .last()
                    .name,
            )
            val recreated = CharactersViewModel(observe)
            advanceUntilIdle()
            assertEquals(
                "New hero",
                recreated.uiState.value.characters
                    .last()
                    .name,
            )
        }

    @Test fun observingFailureIsExposedByBothViews() =
        runTest(dispatcher) {
            val failing =
                object : CharacterRepository {
                    override suspend fun save(character: com.dozy.mobile.domain.Character): SavedCharacter = error("unused")

                    override fun observe() =
                        kotlinx.coroutines.flow.flow<List<com.dozy.mobile.domain.Character>> { error("disk unavailable") }
                }
            val home = HomeViewModel(ObserveCharacters(failing))
            val characters = CharactersViewModel(ObserveCharacters(failing))
            advanceUntilIdle()
            assertTrue(home.uiState.value.characterLoadError)
            assertTrue(characters.uiState.value.characterLoadError)
        }

    private fun viewModel(handle: SavedStateHandle = SavedStateHandle()) =
        CreateCharacterViewModel(
            handle,
            SaveCharacter(repository, validator, CharacterIdGenerator { CharacterId("new") }),
            validator,
        )
}
