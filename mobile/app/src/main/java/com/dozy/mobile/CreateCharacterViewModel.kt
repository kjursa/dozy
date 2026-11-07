package com.dozy.mobile

import androidx.lifecycle.SavedStateHandle
import com.kjursa.composeuikit.mvi.MviViewModel

private const val KEY_SELECTED_TYPE = "createCharacter.selectedType"
private const val KEY_NAME = "createCharacter.name"
private const val KEY_PERSONALITY = "createCharacter.personality"
private const val KEY_APPEARANCE = "createCharacter.appearance"

internal data class CreateCharacterUiState(
    val selectedType: String = "Child",
    val name: String = "Oliver",
    val personality: String = "Curious and brave, loves exploring mysterious places and asking ‘what’s over there?’",
    val appearance: String = "Five years old with curly brown hair, bright green eyes, a yellow hoodie and blue sneakers.",
)

internal sealed interface CreateCharacterAction {
    data object BackClicked : CreateCharacterAction
    data object SaveClicked : CreateCharacterAction
    data object PreviewClicked : CreateCharacterAction
    data class TypeSelected(val type: String) : CreateCharacterAction
    data class NameChanged(val name: String) : CreateCharacterAction
    data class PersonalityChanged(val personality: String) : CreateCharacterAction
    data class AppearanceChanged(val appearance: String) : CreateCharacterAction
}

internal sealed interface CreateCharacterEffect {
    data object NavigateBack : CreateCharacterEffect
    data object SaveRequested : CreateCharacterEffect
    data object PreviewRequested : CreateCharacterEffect
}

internal class CreateCharacterViewModel(
    private val savedStateHandle: SavedStateHandle,
) : MviViewModel<CreateCharacterUiState, CreateCharacterAction, CreateCharacterEffect>(savedStateHandle.toUiState()) {

    override fun onAction(action: CreateCharacterAction) {
        when (action) {
            CreateCharacterAction.BackClicked -> sendEffect(CreateCharacterEffect.NavigateBack)
            CreateCharacterAction.SaveClicked -> sendEffect(CreateCharacterEffect.SaveRequested)
            CreateCharacterAction.PreviewClicked -> sendEffect(CreateCharacterEffect.PreviewRequested)
            is CreateCharacterAction.TypeSelected -> updateCharacterState { copy(selectedType = action.type) }
            is CreateCharacterAction.NameChanged -> updateCharacterState { copy(name = action.name) }
            is CreateCharacterAction.PersonalityChanged -> updateCharacterState { copy(personality = action.personality) }
            is CreateCharacterAction.AppearanceChanged -> updateCharacterState { copy(appearance = action.appearance) }
        }
    }

    private fun updateCharacterState(reducer: CreateCharacterUiState.() -> CreateCharacterUiState) {
        val next = uiState.value.reducer()
        savedStateHandle[KEY_SELECTED_TYPE] = next.selectedType
        savedStateHandle[KEY_NAME] = next.name
        savedStateHandle[KEY_PERSONALITY] = next.personality
        savedStateHandle[KEY_APPEARANCE] = next.appearance
        updateState { next }
    }

}

private fun SavedStateHandle.toUiState() = CreateCharacterUiState(
    selectedType = this[KEY_SELECTED_TYPE] ?: "Child",
    name = this[KEY_NAME] ?: "Oliver",
    personality = this[KEY_PERSONALITY]
        ?: "Curious and brave, loves exploring mysterious places and asking ‘what’s over there?’",
    appearance = this[KEY_APPEARANCE]
        ?: "Five years old with curly brown hair, bright green eyes, a yellow hoodie and blue sneakers.",
)
