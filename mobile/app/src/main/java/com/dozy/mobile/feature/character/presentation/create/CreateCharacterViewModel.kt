package com.dozy.mobile.feature.character.presentation.create

import androidx.lifecycle.viewModelScope
import com.dozy.mobile.feature.character.domain.model.CreateCharacterValidation
import com.dozy.mobile.feature.character.domain.usecase.SaveCharacter
import com.kjursa.foundation.compose.mvi.MviViewModel
import com.kjursa.foundation.compose.mvi.StatePersistence
import kotlinx.coroutines.launch

internal class CreateCharacterViewModel(
    statePersistence: StatePersistence<CreateCharacterUiState>,
    private val saveCharacter: SaveCharacter,
) : MviViewModel<CreateCharacterUiState, CreateCharacterAction, CreateCharacterEffect>(
        initialState = CreateCharacterUiState(),
        statePersistence = statePersistence,
    ) {
    override fun onAction(action: CreateCharacterAction) {
        when (action) {
            is CreateCharacterAction.InitializeSamples ->
                updateState {
                    withSampleDefaults(action.name, action.personality, action.appearance)
                }
            CreateCharacterAction.BackClicked -> sendEffect(CreateCharacterEffect.NavigateBack)
            CreateCharacterAction.SaveClicked -> save()
            CreateCharacterAction.PreviewClicked -> sendEffect(CreateCharacterEffect.PreviewRequested)
            is CreateCharacterAction.TypeSelected -> updateCharacterState { withType(action.type) }
            is CreateCharacterAction.NameChanged -> updateCharacterState { withName(action.name) }
            is CreateCharacterAction.PersonalityChanged -> updateCharacterState { withPersonality(action.personality) }
            is CreateCharacterAction.AppearanceChanged -> updateCharacterState { withAppearance(action.appearance) }
        }
    }

    private fun save() {
        if (uiState.value.saveStatus == SaveStatus.SAVING) return
        val input =
            when (val validation = uiState.value.validateInput()) {
                is CreateCharacterValidation.Valid -> validation.input
                is CreateCharacterValidation.Invalid -> {
                    updateState { withValidationErrors(validation.errors).copy(saveStatus = SaveStatus.EDITING) }
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
                is CreateCharacterValidation.Invalid -> edited.withValidationErrors(validation.errors)
            }
        val next = validated.copy(saveStatus = SaveStatus.EDITING)
        updateState { next }
    }
}
