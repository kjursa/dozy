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
            assertEquals("Draft", restored.uiState.value.draft.name)
            assertEquals("asset", restored.uiState.value.draft.avatarId)
            assertFalse(restored.uiState.value is CreateCharacterUiState.Generating)
            assertNull(restored.uiState.value.draft.nameError)
            assertTrue(restored.uiState.value is CreateCharacterUiState.AvatarReady)
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
            assertTrue(restored.uiState.value is CreateCharacterUiState.Loading)
            advanceUntilIdle()
            assertEquals("Draft", restored.uiState.value.draft.personality)
            assertTrue(restored.uiState.value.draft.avatarOutdated)
            stored.characters.value = emptyList()
            restored.dispatch(CreateCharacterAction.SaveClicked)
            advanceUntilIdle()
            assertTrue(restored.uiState.value is CreateCharacterUiState.Unavailable)
            assertEquals(0, stored.saves)
            val missing = restored()
            advanceUntilIdle()
            assertTrue(missing.uiState.value is CreateCharacterUiState.Unavailable)
            assertFalse(missing.uiState.value is CreateCharacterUiState.AvatarReady)
        }
}
