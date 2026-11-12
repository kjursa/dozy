package com.dozy.mobile

import androidx.lifecycle.SavedStateHandle
import com.dozy.mobile.feature.character.domain.usecase.*
import com.dozy.mobile.feature.character.presentation.create.*
import com.dozy.mobile.resources.*
import com.kjursa.foundation.compose.mvi.SavedStatePersistence
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class CreateCharacterRestorationTest : CreateCharacterViewModelFixture() {
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

    @Test fun restoredEditKeepsDraftAndRechecksExistenceBeforeSaving() =
        runTest(dispatcher) {
            stored.characters.value = listOf(character().copy(avatar = avatar()))
            val handle = SavedStateHandle()
            val vm = vm(handle, "1")
            advanceUntilIdle()
            vm.dispatch(CreateCharacterAction.PersonalityChanged("Draft"))
            val snapshot = handle.get<String>(SavedStatePersistence.DEFAULT_KEY)

            fun restored() = vm(SavedStateHandle(mapOf(SavedStatePersistence.DEFAULT_KEY to snapshot)), "1")
            val restored = restored()
            assertTrue(restored.uiState.value.loading)
            advanceUntilIdle()
            assertEquals("Draft", restored.uiState.value.personality)
            assertTrue(restored.uiState.value.avatarOutdated)
            stored.characters.value = emptyList()
            restored.dispatch(CreateCharacterAction.SaveClicked)
            advanceUntilIdle()
            assertTrue(restored.uiState.value.unavailable)
            assertEquals(0, stored.saves)
            val missing = restored()
            advanceUntilIdle()
            assertTrue(missing.uiState.value.unavailable)
            assertFalse(missing.uiState.value.canSave)
        }
}
