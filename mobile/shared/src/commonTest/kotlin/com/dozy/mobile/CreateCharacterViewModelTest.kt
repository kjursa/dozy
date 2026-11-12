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
            assertEquals(UiText.Resource(Res.string.avatar_failed), vm.uiState.value.generationError)
            assertEquals("Finn", vm.uiState.value.name)
            assertFalse(vm.uiState.value.canSave)
            generate = { null }
            ready(vm)
            assertEquals(UiText.Resource(Res.string.avatar_unavailable), vm.uiState.value.generationError)
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

    @Test fun missingAndReadFailureHaveDistinctStatesAndRetryReloads() =
        runTest(dispatcher) {
            val missing = vm(id = "absent")
            assertTrue(missing.uiState.value.loading)
            missing.dispatch(CreateCharacterAction.SaveClicked)
            advanceUntilIdle()
            assertTrue(missing.uiState.value.unavailable)
            assertNull(missing.uiState.value.loadError)
            assertEquals(0, stored.saves)
            readFailure = IllegalStateException("disk read")
            val failed = vm(id = "1")
            advanceUntilIdle()
            assertFalse(failed.uiState.value.unavailable)
            assertNotNull(failed.uiState.value.loadError)
            failed.dispatch(CreateCharacterAction.NameChanged("Ignored"))
            assertNull(failed.uiState.value.name)
            readFailure = null
            failed.dispatch(CreateCharacterAction.RetryLoadClicked)
            failed.dispatch(CreateCharacterAction.RetryLoadClicked)
            advanceUntilIdle()
            assertNull(failed.uiState.value.loadError)
            assertEquals("Oliver", failed.uiState.value.name)
        }

    @Test fun legacyInputIsReadableButMustBeCorrectedBeforeSaving() =
        runTest(dispatcher) {
            stored.characters.value = listOf(character(name = "x".repeat(55)).copy(avatar = avatar()))
            val vm = vm(id = "1")
            advanceUntilIdle()
            assertEquals("x".repeat(55), vm.uiState.value.name)
            assertFalse(vm.uiState.value.canSave)
            vm.dispatch(CreateCharacterAction.SaveClicked)
            assertNotNull(vm.uiState.value.nameError)
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
            assertFalse(vm.uiState.value.avatarOutdated)
            vm.dispatch(CreateCharacterAction.PersonalityChanged("Curious"))
            assertTrue(vm.uiState.value.avatarOutdated)
            generate = { error("failed") }
            ready(vm)
            assertEquals("asset", vm.uiState.value.avatarId)
            assertTrue(vm.uiState.value.avatarOutdated)
            generate = { Character.Avatar(Character.Avatar.Id("replacement"), avatar().url) }
            ready(vm)
            assertFalse(vm.uiState.value.avatarOutdated)
            assertEquals("replacement", vm.uiState.value.avatarId)
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
            assertEquals(CreateCharacterEffect.Saved(original.id), vm.effects.first())
        }
}
