package com.dozy.mobile

import com.dozy.mobile.feature.character.domain.model.Character
import com.dozy.mobile.feature.character.domain.usecase.*
import com.dozy.mobile.feature.character.presentation.create.*
import com.dozy.mobile.resources.*
import com.kjursa.foundation.presentation.text.UiText
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
internal class CreateCharacterViewModelTest : CreateCharacterViewModelFixture() {
    @Test fun startsEmptyAndRequiresValidExplicitGenerationBeforeSave() =
        runTest(dispatcher) {
            val vm = vm()
            assertEquals(
                "",
                vm.uiState.value.draft.displayName
                    .let { (it as UiText.Plain).value },
            )
            vm.dispatch(CreateCharacterAction.PreviewClicked)
            assertNotNull(vm.uiState.value.draft.nameError)
            assertNotNull(vm.uiState.value.draft.personalityError)
            fill(vm)
            vm.dispatch(CreateCharacterAction.SaveClicked)
            advanceUntilIdle()
            assertEquals(0, generations)
            assertEquals(0, stored.saves)
            assertFalse(vm.uiState.value is CreateCharacterUiState.AvatarReady)
            vm.dispatch(CreateCharacterAction.PreviewClicked)
            advanceUntilIdle()
            assertTrue(vm.uiState.value is CreateCharacterUiState.AvatarReady)
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
            assertTrue(vm.uiState.value is CreateCharacterUiState.Generating)
            assertFalse(vm.uiState.value is CreateCharacterUiState.AvatarReady)
            assertEquals("Finn", vm.uiState.value.draft.name)
            assertEquals("Child", vm.uiState.value.draft.selectedType)
            assertEquals("Green eyes", vm.uiState.value.draft.appearance)
            assertEquals(1, generations)
            assertEquals(0, stored.saves)
            gate.complete(avatar())
            advanceUntilIdle()
            assertFalse(vm.uiState.value is CreateCharacterUiState.Generating)
            assertTrue(vm.uiState.value is CreateCharacterUiState.AvatarReady)
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
            assertTrue(vm.uiState.value is CreateCharacterUiState.AvatarReady)
            assertEquals("asset", vm.uiState.value.draft.avatarId)
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
            assertEquals(CreateCharacterDestination.Saved(Character.Id("new")), vm.navigator.commands.first())
            vm.dispatch(CreateCharacterAction.SaveClicked)
            advanceUntilIdle()
            assertEquals(1, stored.saves)
            assertEquals(1, generations)
        }

    @Test fun failedCreateKeepsAnIdlessDraftWithoutAddingACharacter() =
        runTest(dispatcher) {
            val initialCharacters = stored.characters.value
            val form = vm()
            fill(form)
            ready(form)
            stored.failure = IllegalStateException("Offline")
            form.dispatch(CreateCharacterAction.SaveClicked)
            advanceUntilIdle()

            assertTrue((form.uiState.value as CreateCharacterUiState.AvatarReady).saveFailed)
            assertEquals(initialCharacters, stored.characters.value)
            val key = assertNotNull(form.uiState.value.draft.creationKey)
            val reopened = vm()
            advanceUntilIdle()
            assertEquals(key, reopened.uiState.value.draft.creationKey)
            assertEquals("Finn", reopened.uiState.value.draft.name)
            assertEquals("Curious", reopened.uiState.value.draft.personality)
            assertNull(reopened.uiState.value.draft.editingId)

            stored.failure = null
            reopened.dispatch(CreateCharacterAction.SaveClicked)
            advanceUntilIdle()
            assertEquals(CreateCharacterDestination.Saved(Character.Id("new")), reopened.navigator.commands.first())
            assertNull(draft.load())
        }

    @Test fun generationFailureAndUnavailableRetainInputAndAllowRetryWithoutSaving() =
        runTest(dispatcher) {
            val vm = vm()
            fill(vm)
            generate = { error("HTTP 500") }
            ready(vm)
            assertEquals(UiText.Resource(Res.string.avatar_failed), (vm.uiState.value as? CreateCharacterUiState.Editable)?.generationError)
            assertEquals("Finn", vm.uiState.value.draft.name)
            assertFalse(vm.uiState.value is CreateCharacterUiState.AvatarReady)
            generate = { null }
            ready(vm)
            assertEquals(UiText.Resource(Res.string.avatar_unavailable), (vm.uiState.value as? CreateCharacterUiState.Editable)?.generationError)
            generate = { avatar() }
            ready(vm)
            assertNull((vm.uiState.value as? CreateCharacterUiState.Editable)?.generationError)
            assertTrue(vm.uiState.value is CreateCharacterUiState.AvatarReady)
            generate = { error("regeneration failed") }
            ready(vm)
            assertEquals("asset", vm.uiState.value.draft.avatarId)
            assertTrue(vm.uiState.value is CreateCharacterUiState.AvatarReady)
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
            assertEquals(CreateCharacterDestination.NavigateBack, vm.navigator.commands.first())
            gate.complete(avatar())
            advanceUntilIdle()
            assertNull(vm.uiState.value.draft.avatarId)
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
            assertEquals("new-asset", vm.uiState.value.draft.avatarId)
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
            assertTrue((vm.uiState.value as CreateCharacterUiState.AvatarReady).saveFailed)
            assertEquals("asset", vm.uiState.value.draft.avatarId)
            stored.failure = null
            vm.dispatch(CreateCharacterAction.SaveClicked)
            advanceUntilIdle()
            assertEquals(CreateCharacterDestination.Saved(Character.Id("new")), vm.navigator.commands.first())
        }

    @Test fun editingReusesAvatarAndDoesNotWriteUntilSave() =
        runTest(dispatcher) {
            stored.save(character().copy(avatar = avatar()))
            val vm = vm(id = "1")
            advanceUntilIdle()
            assertEquals("1", vm.uiState.value.draft.editingId)
            vm.dispatch(CreateCharacterAction.NameChanged("Edited"))
            assertTrue(vm.uiState.value is CreateCharacterUiState.AvatarReady)
            assertEquals(
                "Oliver",
                stored.characters.value
                    .first { it.id.value == "1" }
                    .details.name.value,
            )
            vm.dispatch(CreateCharacterAction.SaveClicked)
            advanceUntilIdle()
            assertEquals(CreateCharacterDestination.Saved(Character.Id("1")), vm.navigator.commands.first())
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
            assertEquals("asset", vm.uiState.value.draft.avatarId)
            assertEquals(1, stored.saves)
        }

    @Test fun missingAndReadFailureHaveDistinctStatesAndRetryReloads() =
        runTest(dispatcher) {
            val missing = vm(id = "absent")
            assertTrue(missing.uiState.value is CreateCharacterUiState.Loading)
            missing.dispatch(CreateCharacterAction.SaveClicked)
            advanceUntilIdle()
            assertTrue(missing.uiState.value is CreateCharacterUiState.Unavailable)
            assertNull((missing.uiState.value as? CreateCharacterUiState.LoadFailed)?.error)
            assertEquals(0, stored.saves)
            readFailure = IllegalStateException("disk read")
            val failed = vm(id = "1")
            advanceUntilIdle()
            assertFalse(failed.uiState.value is CreateCharacterUiState.Unavailable)
            assertNotNull((failed.uiState.value as? CreateCharacterUiState.LoadFailed)?.error)
            failed.dispatch(CreateCharacterAction.NameChanged("Ignored"))
            assertNull(failed.uiState.value.draft.name)
            readFailure = null
            failed.dispatch(CreateCharacterAction.RetryLoadClicked)
            failed.dispatch(CreateCharacterAction.RetryLoadClicked)
            advanceUntilIdle()
            assertNull((failed.uiState.value as? CreateCharacterUiState.LoadFailed)?.error)
            assertEquals("Oliver", failed.uiState.value.draft.name)
        }

    @Test fun legacyInputIsReadableButMustBeCorrectedBeforeSaving() =
        runTest(dispatcher) {
            stored.characters.value = listOf(character(name = "x".repeat(55)).copy(avatar = avatar()))
            val vm = vm(id = "1")
            advanceUntilIdle()
            assertEquals("x".repeat(55), vm.uiState.value.draft.name)
            assertFalse(vm.uiState.value is CreateCharacterUiState.AvatarReady)
            vm.dispatch(CreateCharacterAction.SaveClicked)
            assertNotNull(vm.uiState.value.draft.nameError)
            assertEquals(0, stored.saves)
            vm.dispatch(CreateCharacterAction.NameChanged("Corrected"))
            vm.dispatch(CreateCharacterAction.SaveClicked)
            advanceUntilIdle()
            assertEquals(
                "1",
                stored.characters.value
                    .single()
                    .id.value,
            )
            assertEquals(
                "Corrected",
                stored.characters.value
                    .single()
                    .details.name.value,
            )
        }

    @Test fun editRegenerationMarksOldPreviewAndReplacesItOnlyOnSuccess() =
        runTest(dispatcher) {
            stored.characters.value = listOf(character().copy(avatar = avatar()))
            val original = stored.characters.value.single()
            val vm = vm(id = "1")
            advanceUntilIdle()
            assertFalse(vm.uiState.value.draft.avatarOutdated)
            vm.dispatch(CreateCharacterAction.PersonalityChanged("Curious"))
            assertTrue(vm.uiState.value.draft.avatarOutdated)
            generate = { error("failed") }
            ready(vm)
            assertEquals("asset", vm.uiState.value.draft.avatarId)
            assertTrue(vm.uiState.value.draft.avatarOutdated)
            generate = { Character.Avatar(Character.Avatar.Id("replacement"), avatar().url) }
            ready(vm)
            assertFalse(vm.uiState.value.draft.avatarOutdated)
            assertEquals("replacement", vm.uiState.value.draft.avatarId)
            assertEquals(original, stored.characters.value.single())
            vm.dispatch(CreateCharacterAction.SaveClicked)
            advanceUntilIdle()
            assertEquals(
                "replacement",
                stored.characters.value
                    .single()
                    .avatar
                    ?.id
                    ?.value,
            )
            assertEquals(CreateCharacterDestination.Saved(original.id), vm.navigator.commands.first())
        }
}
