package com.dozy.mobile

import androidx.lifecycle.SavedStateHandle
import com.dozy.mobile.feature.character.domain.model.Character
import com.dozy.mobile.feature.character.domain.model.CreateCharacterInput
import com.dozy.mobile.feature.character.domain.model.SavedCharacter
import com.dozy.mobile.feature.character.domain.repository.CharacterRepository
import com.dozy.mobile.feature.character.domain.usecase.*
import com.dozy.mobile.feature.character.presentation.create.*
import com.kjursa.foundation.compose.mvi.SavedStatePersistence
import com.kjursa.foundation.presentation.text.UiText
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CreateCharacterViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val stored = FakeCharacterRepository()
    private var generations = 0
    private var generate: suspend () -> Character.Avatar? = { avatar() }
    private val repository =
        object : CharacterRepository {
            override suspend fun delete(
                id: com.dozy.mobile.feature.character.domain.model.Character.Id,
            ): com.dozy.mobile.feature.character.domain.model.CharacterSync = error("Unexpected deletion")

            override fun observe() = stored.observe()

            override suspend fun save(character: Character): SavedCharacter = stored.save(character)

            override suspend fun generateAvatar(input: CreateCharacterInput): Character.Avatar? {
                generations++
                assertEquals("Curious", input.personality.value)
                return generate()
            }
        }

    @Before fun setup() {
        Dispatchers.setMain(dispatcher)
    }

    @After fun teardown() {
        Dispatchers.resetMain()
    }

    @Test fun startsEmptyAndRequiresValidExplicitGenerationBeforeSave() =
        runTest(dispatcher) {
            val vm = vm()
            assertEquals(
                "",
                vm.uiState.value.displayName
                    .let { (it as UiText.Plain).value },
            )
            vm.dispatch(CreateCharacterAction.PreviewClicked)
            assertNotNull(vm.uiState.value.nameError)
            assertNotNull(vm.uiState.value.personalityError)
            fill(vm)
            vm.dispatch(CreateCharacterAction.SaveClicked)
            advanceUntilIdle()
            assertEquals(0, generations)
            assertEquals(0, stored.saves)
            assertFalse(vm.uiState.value.canSave)
            vm.dispatch(CreateCharacterAction.PreviewClicked)
            advanceUntilIdle()
            assertTrue(vm.uiState.value.canSave)
            assertEquals(0, stored.saves)
        }

    @Test fun generationBlocksEditsSaveAndDuplicateRequests() =
        runTest(dispatcher) {
            val gate = CompletableDeferred<Character.Avatar?>()
            generate = { gate.await() }
            val vm = vm()
            fill(vm)
            vm.dispatch(CreateCharacterAction.PreviewClicked)
            vm.dispatch(CreateCharacterAction.NameChanged("Ignored"))
            vm.dispatch(CreateCharacterAction.PersonalityChanged("Ignored"))
            vm.dispatch(CreateCharacterAction.TypeSelected("Animal"))
            vm.dispatch(CreateCharacterAction.AppearanceChanged("Ignored"))
            vm.dispatch(CreateCharacterAction.PreviewClicked)
            vm.dispatch(CreateCharacterAction.SaveClicked)
            runCurrent()
            assertTrue(vm.uiState.value.generating)
            assertFalse(vm.uiState.value.canSave)
            assertEquals("Finn", vm.uiState.value.name)
            assertEquals("Child", vm.uiState.value.selectedType)
            assertEquals("Green eyes", vm.uiState.value.appearance)
            assertEquals(1, generations)
            assertEquals(0, stored.saves)
            gate.complete(avatar())
            advanceUntilIdle()
            assertFalse(vm.uiState.value.generating)
            assertTrue(vm.uiState.value.canSave)
        }

    @Test fun changedInputKeepsAvatarAndSaveUsesCurrentInputExactlyOnce() =
        runTest(dispatcher) {
            val vm = vm()
            fill(vm)
            ready(vm)
            vm.dispatch(CreateCharacterAction.NameChanged("  New name  "))
            vm.dispatch(CreateCharacterAction.PersonalityChanged("Kind"))
            vm.dispatch(CreateCharacterAction.AppearanceChanged("Blue coat"))
            vm.dispatch(CreateCharacterAction.TypeSelected("Fantasy"))
            assertTrue(vm.uiState.value.canSave)
            assertEquals("asset", vm.uiState.value.avatarId)
            val gate = CompletableDeferred<Unit>()
            stored.beforeSave = { gate.await() }
            vm.dispatch(CreateCharacterAction.SaveClicked)
            vm.dispatch(CreateCharacterAction.SaveClicked)
            vm.dispatch(CreateCharacterAction.NameChanged("Ignored"))
            runCurrent()
            assertEquals(1, stored.saves)
            gate.complete(Unit)
            advanceUntilIdle()
            val saved = stored.characters.value.last()
            assertEquals("New name", saved.details.name.value)
            assertEquals("Kind", saved.details.personality.value)
            assertEquals("Blue coat", saved.details.appearance.value)
            assertEquals(Character.Type.FANTASY, saved.details.type)
            assertEquals(avatar(), saved.avatar)
            assertEquals(CreateCharacterEffect.Saved(Character.Id("new")), vm.effects.first())
            vm.dispatch(CreateCharacterAction.SaveClicked)
            advanceUntilIdle()
            assertEquals(1, stored.saves)
            assertEquals(1, generations)
        }

    @Test fun generationFailureAndUnavailableRetainInputAndAllowRetryWithoutSaving() =
        runTest(dispatcher) {
            val vm = vm()
            fill(vm)
            generate = { error("HTTP 500") }
            ready(vm)
            assertEquals(UiText.Resource(R.string.avatar_failed), vm.uiState.value.generationError)
            assertEquals("Finn", vm.uiState.value.name)
            assertFalse(vm.uiState.value.canSave)
            generate = { null }
            ready(vm)
            assertEquals(UiText.Resource(R.string.avatar_unavailable), vm.uiState.value.generationError)
            generate = { avatar() }
            ready(vm)
            assertNull(vm.uiState.value.generationError)
            assertTrue(vm.uiState.value.canSave)
            generate = { error("regeneration failed") }
            ready(vm)
            assertEquals("asset", vm.uiState.value.avatarId)
            assertTrue(vm.uiState.value.canSave)
            assertEquals(0, stored.saves)
        }

    @Test fun abandonedNonCooperativeResponseCannotChangePreviewOrSave() =
        runTest(dispatcher) {
            val gate = CompletableDeferred<Character.Avatar?>()
            generate = { withContext(NonCancellable) { gate.await() } }
            val vm = vm()
            fill(vm)
            vm.dispatch(CreateCharacterAction.PreviewClicked)
            runCurrent()
            vm.dispatch(CreateCharacterAction.BackClicked)
            assertEquals(CreateCharacterEffect.NavigateBack, vm.effects.first())
            gate.complete(avatar())
            advanceUntilIdle()
            assertNull(vm.uiState.value.avatarId)
            vm.dispatch(CreateCharacterAction.SaveClicked)
            advanceUntilIdle()
            assertEquals(0, stored.saves)
        }

    @Test fun supersededResponseCannotReplaceNewAttempt() =
        runTest(dispatcher) {
            val gate = CompletableDeferred<Character.Avatar?>()
            generate = { withContext(NonCancellable) { gate.await() } }
            val vm = vm()
            fill(vm)
            vm.dispatch(CreateCharacterAction.PreviewClicked)
            runCurrent()
            vm.abandon()
            vm.resume()
            val replacement = Character.Avatar(Character.Avatar.Id("new-asset"), avatar().url)
            generate = { replacement }
            ready(vm)
            gate.complete(avatar())
            advanceUntilIdle()
            assertEquals("new-asset", vm.uiState.value.avatarId)
            assertEquals(0, stored.saves)
        }

    @Test fun saveFailureRetainsAvatarAndCanRetry() =
        runTest(dispatcher) {
            val vm = vm()
            fill(vm)
            ready(vm)
            stored.failure = IllegalStateException("disk full")
            vm.dispatch(CreateCharacterAction.SaveClicked)
            advanceUntilIdle()
            assertEquals(SaveStatus.FAILURE, vm.uiState.value.saveStatus)
            assertEquals("asset", vm.uiState.value.avatarId)
            stored.failure = null
            vm.dispatch(CreateCharacterAction.SaveClicked)
            advanceUntilIdle()
            assertEquals(CreateCharacterEffect.Saved(Character.Id("new")), vm.effects.first())
        }

    @Test fun recreationRestoresInputAndAvatarButNoErrorsOrActiveGeneration() =
        runTest(dispatcher) {
            val handle = SavedStateHandle()
            val vm = vm(handle)
            fill(vm)
            ready(vm)
            vm.dispatch(CreateCharacterAction.NameChanged(""))
            generate = { awaitCancellation() }
            vm.dispatch(CreateCharacterAction.NameChanged("Draft"))
            vm.dispatch(CreateCharacterAction.PreviewClicked)
            runCurrent()
            val restored =
                vm(SavedStateHandle(mapOf(SavedStatePersistence.DEFAULT_KEY to handle.get<String>(SavedStatePersistence.DEFAULT_KEY))))
            assertEquals("Draft", restored.uiState.value.name)
            assertEquals("asset", restored.uiState.value.avatarId)
            assertFalse(restored.uiState.value.generating)
            assertNull(restored.uiState.value.nameError)
            assertEquals(SaveStatus.EDITING, restored.uiState.value.saveStatus)
            vm.abandon()
        }

    @Test fun editingReusesAvatarAndDoesNotWriteUntilSave() =
        runTest(dispatcher) {
            stored.save(character().copy(avatar = avatar()))
            val vm = vm(id = "1")
            advanceUntilIdle()
            assertEquals("1", vm.uiState.value.editingId)
            vm.dispatch(CreateCharacterAction.NameChanged("Edited"))
            assertTrue(vm.uiState.value.canSave)
            assertEquals(
                "Oliver",
                stored.characters.value
                    .first { it.id.value == "1" }
                    .details.name.value,
            )
            vm.dispatch(CreateCharacterAction.SaveClicked)
            advanceUntilIdle()
            assertEquals(CreateCharacterEffect.Saved(Character.Id("1")), vm.effects.first())
            assertEquals(
                "Edited",
                stored.characters.value
                    .last()
                    .details.name.value,
            )
            assertEquals(0, generations)
        }

    @Test fun leavingEditDuringGenerationKeepsExistingSavedCharacter() =
        runTest(dispatcher) {
            stored.save(character().copy(avatar = avatar()))
            val original = stored.characters.value
            val gate = CompletableDeferred<Character.Avatar?>()
            generate = { withContext(NonCancellable) { gate.await() } }
            val vm = vm(id = "1")
            runCurrent()
            vm.dispatch(CreateCharacterAction.PersonalityChanged("Curious"))
            vm.dispatch(CreateCharacterAction.PreviewClicked)
            runCurrent()
            vm.dispatch(CreateCharacterAction.BackClicked)
            gate.complete(Character.Avatar(Character.Avatar.Id("replacement"), avatar().url))
            advanceUntilIdle()
            assertEquals(original, stored.characters.value)
            assertEquals("asset", vm.uiState.value.avatarId)
            assertEquals(1, stored.saves)
        }

    private suspend fun TestScope.ready(vm: CreateCharacterViewModel) {
        vm.dispatch(CreateCharacterAction.PreviewClicked)
        runCurrent()
    }

    private fun fill(vm: CreateCharacterViewModel) {
        vm.dispatch(CreateCharacterAction.NameChanged("Finn"))
        vm.dispatch(CreateCharacterAction.PersonalityChanged("Curious"))
        vm.dispatch(CreateCharacterAction.AppearanceChanged("Green eyes"))
    }

    private fun vm(
        handle: SavedStateHandle = SavedStateHandle(),
        id: String? = null,
    ) = CreateCharacterViewModel(
        SavedStatePersistence(handle, CreateCharacterUiState.serializer()),
        SaveCharacter(repository, CharacterIdGenerator { Character.Id("new") }),
        GenerateCharacterAvatar(repository),
        ObserveCharacters(repository),
        id,
    )
}
