package com.dozy.mobile

import androidx.lifecycle.SavedStateHandle
import com.dozy.mobile.feature.character.domain.model.Character
import com.dozy.mobile.feature.character.domain.model.SavedCharacter
import com.dozy.mobile.feature.character.domain.repository.CharacterRepository
import com.dozy.mobile.feature.character.domain.usecase.CharacterIdGenerator
import com.dozy.mobile.feature.character.domain.usecase.ObserveCharacters
import com.dozy.mobile.feature.character.domain.usecase.SaveCharacter
import com.dozy.mobile.feature.character.presentation.create.CreateCharacterAction
import com.dozy.mobile.feature.character.presentation.create.CreateCharacterEffect
import com.dozy.mobile.feature.character.presentation.create.CreateCharacterUiState
import com.dozy.mobile.feature.character.presentation.create.CreateCharacterViewModel
import com.dozy.mobile.feature.character.presentation.create.SaveStatus
import com.dozy.mobile.feature.character.presentation.list.CharactersViewModel
import com.dozy.mobile.feature.home.presentation.HomeViewModel
import com.kjursa.foundation.compose.mvi.SavedStatePersistence
import com.kjursa.foundation.presentation.text.UiText
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
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CreateCharacterViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val repository = FakeCharacterRepository()

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
            assertEquals(UiText.Resource(R.string.character_name_required), vm.uiState.value.nameError)
            assertEquals(UiText.Resource(R.string.character_description_required), vm.uiState.value.personalityError)
            assertEquals(UiText.Resource(R.string.character_description_required), vm.uiState.value.appearanceError)
            assertEquals(UiText.Resource(R.string.character_type_required), vm.uiState.value.typeError)
            assertEquals(0, repository.saves)
            vm.dispatch(CreateCharacterAction.NameChanged("Finn"))
            vm.dispatch(CreateCharacterAction.PersonalityChanged("Kind"))
            vm.dispatch(CreateCharacterAction.AppearanceChanged("Green eyes"))
            vm.dispatch(CreateCharacterAction.TypeSelected("Fantasy"))
            assertNoFieldErrors(vm)
            assertEquals("Finn", viewModel(handle).uiState.value.name)
            assertEquals("Fantasy", viewModel(handle).uiState.value.selectedType)
            assertEquals("Kind", viewModel(handle).uiState.value.personality)
            assertEquals("Green eyes", viewModel(handle).uiState.value.appearance)
        }

    @Test fun successfulSaveClosesImmediatelyAndAvoidsDuplicateSaves() =
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
            // Success emits navigation without another Save/Done click.
            assertEquals(CreateCharacterEffect.Saved, vm.effects.first())
            vm.dispatch(CreateCharacterAction.SaveClicked)
            advanceUntilIdle()
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
            assertEquals(CreateCharacterEffect.Saved, vm.effects.first())
        }

    @Test fun invalidNameNeverStartsSavingAndErrorsClearOnCorrection() =
        runTest(dispatcher) {
            val vm = viewModel()
            for (name in listOf("", "a".repeat(51), "Captain\nFinn")) {
                vm.dispatch(CreateCharacterAction.NameChanged(name))
                vm.dispatch(CreateCharacterAction.SaveClicked)
                assertEquals(SaveStatus.EDITING, vm.uiState.value.saveStatus)
                assertNotNull(vm.uiState.value.nameError)
                assertNull(vm.uiState.value.personalityError)
                assertNull(vm.uiState.value.appearanceError)
                assertNull(vm.uiState.value.typeError)
            }
            advanceUntilIdle()
            assertEquals(0, repository.saves)
            vm.dispatch(CreateCharacterAction.NameChanged("  Finn  "))
            assertNoFieldErrors(vm)
            vm.dispatch(CreateCharacterAction.SaveClicked)
            advanceUntilIdle()
            assertEquals(
                "Finn",
                repository.characters.value
                    .last()
                    .details.name.value,
            )
            assertEquals(CreateCharacterEffect.Saved, vm.effects.first())
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
                    override suspend fun save(character: com.dozy.mobile.feature.character.domain.model.Character): SavedCharacter =
                        error("unused")

                    override fun observe() =
                        kotlinx.coroutines.flow.flow<List<com.dozy.mobile.feature.character.domain.model.Character>> {
                            error(
                                "disk unavailable",
                            )
                        }
                }
            val home = HomeViewModel(ObserveCharacters(failing))
            val characters = CharactersViewModel(ObserveCharacters(failing))
            advanceUntilIdle()
            assertTrue(home.uiState.value.characterLoadError)
            assertTrue(characters.uiState.value.characterLoadError)
        }

    @Test fun recreationRestoresInvalidTextWithoutPersistingFieldErrors() =
        runTest(dispatcher) {
            val handle = SavedStateHandle()
            val vm = viewModel(handle)
            vm.dispatch(CreateCharacterAction.NameChanged(""))
            vm.dispatch(CreateCharacterAction.PersonalityChanged(""))
            vm.dispatch(CreateCharacterAction.AppearanceChanged("custom appearance"))
            vm.dispatch(CreateCharacterAction.TypeSelected("Unknown"))
            vm.dispatch(CreateCharacterAction.SaveClicked)
            assertNotNull(vm.uiState.value.nameError)
            val restored = viewModel(snapshotHandle(handle)).uiState.value
            assertEquals("", restored.name)
            assertEquals("", restored.personality)
            assertEquals("custom appearance", restored.appearance)
            assertEquals("Unknown", restored.selectedType)
            assertEquals(SaveStatus.EDITING, restored.saveStatus)
            assertNull(restored.nameError)
            assertNull(restored.personalityError)
            assertNull(restored.appearanceError)
            assertNull(restored.typeError)
            assertEquals(0, repository.saves)
        }

    @Test fun recreationDuringSaveRestoresAnEditableFormRatherThanStuckProgress() =
        runTest(dispatcher) {
            val gate = CompletableDeferred<Unit>()
            repository.beforeSave = { gate.await() }
            val handle = SavedStateHandle()
            val vm = viewModel(handle)
            vm.dispatch(CreateCharacterAction.NameChanged("Saved draft"))
            vm.dispatch(CreateCharacterAction.SaveClicked)
            runCurrent()
            assertEquals(SaveStatus.SAVING, vm.uiState.value.saveStatus)
            val restored = viewModel(snapshotHandle(handle))
            assertEquals("Saved draft", restored.uiState.value.name)
            assertEquals(SaveStatus.EDITING, restored.uiState.value.saveStatus)
            assertEquals(1, repository.saves)
            gate.complete(Unit)
            advanceUntilIdle()
        }

    @Test fun recreationDoesNotRestoreAnOldSaveFailure() =
        runTest(dispatcher) {
            repository.failure = IllegalStateException("disk full")
            val handle = SavedStateHandle()
            val vm = viewModel(handle)
            vm.dispatch(CreateCharacterAction.SaveClicked)
            advanceUntilIdle()
            assertEquals(SaveStatus.FAILURE, vm.uiState.value.saveStatus)
            val restored = viewModel(snapshotHandle(handle))
            assertEquals(vm.uiState.value.name, restored.uiState.value.name)
            assertEquals(SaveStatus.EDITING, restored.uiState.value.saveStatus)
        }

    private fun snapshotHandle(handle: SavedStateHandle) =
        SavedStateHandle(
            mapOf(
                SavedStatePersistence.DEFAULT_KEY to requireNotNull(handle.get<String>(SavedStatePersistence.DEFAULT_KEY)),
            ),
        )

    private fun assertNoFieldErrors(vm: CreateCharacterViewModel) {
        val state = vm.uiState.value
        assertNull(state.nameError)
        assertNull(state.personalityError)
        assertNull(state.appearanceError)
        assertNull(state.typeError)
    }

    private fun viewModel(handle: SavedStateHandle = SavedStateHandle()) =
        CreateCharacterViewModel(
            SavedStatePersistence(handle, CreateCharacterUiState.serializer()),
            SaveCharacter(repository, CharacterIdGenerator { Character.Id("new") }),
        )
}
