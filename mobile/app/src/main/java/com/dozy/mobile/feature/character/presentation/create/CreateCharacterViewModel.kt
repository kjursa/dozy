package com.dozy.mobile.feature.character.presentation.create

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.dozy.mobile.feature.character.domain.CreateCharacterValidation
import com.dozy.mobile.feature.character.domain.SaveCharacter
import com.kjursa.foundation.compose.mvi.MviViewModel
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
        val input =
            when (val validation = uiState.value.validateInput()) {
                is CreateCharacterValidation.Valid -> validation.input
                is CreateCharacterValidation.Invalid -> {
                    updateState { withErrors(validation.errors).copy(saveStatus = SaveStatus.EDITING) }
                    return
                }
            }
        updateState { withErrors().copy(saveStatus = SaveStatus.SAVING) }
        viewModelScope.launch {
            when (saveCharacter(input)) {
                is SaveCharacter.Result.Success -> sendEffect(CreateCharacterEffect.Saved)
                SaveCharacter.Result.Failure -> updateState { copy(saveStatus = SaveStatus.FAILURE) }
            }
        }
    }

    private fun updateCharacterState(reducer: CreateCharacterUiState.() -> CreateCharacterUiState) {
        if (uiState.value.saveStatus == SaveStatus.SAVING) return
        val edited = uiState.value.reducer()
        val validated =
            when (val validation = edited.validateInput()) {
                is CreateCharacterValidation.Valid -> edited.withErrors()
                is CreateCharacterValidation.Invalid -> edited.withErrors(validation.errors)
            }
        val next = validated.copy(saveStatus = SaveStatus.EDITING)
        next.saveTo(savedStateHandle)
        updateState { next }
    }
}
