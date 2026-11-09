package com.dozy.mobile.presentation.createcharacter

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.dozy.mobile.domain.character.SaveCharacter
import com.dozy.mobile.domain.character.SaveCharacterResult
import com.kjursa.composeuikit.mvi.MviViewModel
import kotlinx.coroutines.launch

internal class CreateCharacterViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val saveCharacter: SaveCharacter,
) : MviViewModel<CreateCharacterUiState, CreateCharacterAction, CreateCharacterEffect>(savedStateHandle.toUiState()) {
    override fun onAction(action: CreateCharacterAction) {
        when (action) {
            CreateCharacterAction.BackClicked -> sendEffect(CreateCharacterEffect.NavigateBack)
            CreateCharacterAction.SaveClicked -> save()
            CreateCharacterAction.PreviewClicked -> sendEffect(CreateCharacterEffect.PreviewRequested)
            is CreateCharacterAction.TypeSelected -> updateCharacterState { copy(selectedType = action.type) }
            is CreateCharacterAction.NameChanged -> updateCharacterState { copy(name = action.name) }
            is CreateCharacterAction.PersonalityChanged -> updateCharacterState { copy(personality = action.personality) }
            is CreateCharacterAction.AppearanceChanged -> updateCharacterState { copy(appearance = action.appearance) }
        }
    }

    private fun save() {
        if (uiState.value.saveStatus == SaveStatus.SAVING) return
        val (draft, errors) = uiState.value.validateDraft()
        if (draft == null) {
            updateState { copy(saveStatus = SaveStatus.EDITING, errors = errors) }
            return
        }
        updateState { copy(saveStatus = SaveStatus.SAVING, errors = emptyMap()) }
        viewModelScope.launch {
            when (saveCharacter(draft)) {
                is SaveCharacterResult.Success -> sendEffect(CreateCharacterEffect.Saved)
                SaveCharacterResult.Failure -> updateState { copy(saveStatus = SaveStatus.FAILURE) }
            }
        }
    }

    private fun updateCharacterState(reducer: CreateCharacterUiState.() -> CreateCharacterUiState) {
        if (uiState.value.saveStatus == SaveStatus.SAVING) return
        val edited = uiState.value.reducer()
        val next = edited.copy(errors = edited.validateDraft().errors, saveStatus = SaveStatus.EDITING)
        next.saveTo(savedStateHandle)
        updateState { next }
    }
}
