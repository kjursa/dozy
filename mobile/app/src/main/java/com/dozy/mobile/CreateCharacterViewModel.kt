package com.dozy.mobile

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.dozy.mobile.domain.*
import com.kjursa.composeuikit.mvi.MviViewModel
import kotlinx.coroutines.launch

private const val KEY_SELECTED_TYPE = "createCharacter.selectedType"
private const val KEY_NAME = "createCharacter.name"
private const val KEY_PERSONALITY = "createCharacter.personality"
private const val KEY_APPEARANCE = "createCharacter.appearance"

internal enum class SaveStatus { EDITING, SAVING, SUCCESS, FAILURE }

internal data class CreateCharacterUiState(
    val errors: Map<CharacterField, String> = emptyMap(),
    val saveStatus: SaveStatus = SaveStatus.EDITING,
    val sync: CharacterSync? = null,
    val selectedType: String = "Child",
    val name: String = "Oliver",
    val personality: String = "Curious and brave, loves exploring mysterious places and asking ‘what’s over there?’",
    val appearance: String = "Five years old with curly brown hair, bright green eyes, a yellow hoodie and blue sneakers.",
)

internal sealed interface CreateCharacterAction {
    data object BackClicked : CreateCharacterAction

    data object SaveClicked : CreateCharacterAction

    data object PreviewClicked : CreateCharacterAction

    data class TypeSelected(
        val type: String,
    ) : CreateCharacterAction

    data class NameChanged(
        val name: String,
    ) : CreateCharacterAction

    data class PersonalityChanged(
        val personality: String,
    ) : CreateCharacterAction

    data class AppearanceChanged(
        val appearance: String,
    ) : CreateCharacterAction
}

internal sealed interface CreateCharacterEffect {
    data object NavigateBack : CreateCharacterEffect

    data object Saved : CreateCharacterEffect

    data object PreviewRequested : CreateCharacterEffect
}

internal class CreateCharacterViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val saveCharacter: SaveCharacter,
    private val validator: CharacterValidator,
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
        if (uiState.value.saveStatus == SaveStatus.SUCCESS) {
            sendEffect(CreateCharacterEffect.Saved)
            return
        }
        val draft = uiState.value.toDraft()
        val validation = validator.validate(draft)
        if (!validation.isValid) {
            updateState { copy(errors = validation.errors) }
            return
        }
        updateState { copy(saveStatus = SaveStatus.SAVING, errors = emptyMap()) }
        viewModelScope.launch {
            when (val result = saveCharacter(draft)) {
                is SaveCharacterResult.Invalid -> updateState { copy(saveStatus = SaveStatus.EDITING, errors = result.validation.errors) }
                is SaveCharacterResult.Success -> updateState { copy(saveStatus = SaveStatus.SUCCESS, sync = result.saved.sync) }
                SaveCharacterResult.Failure -> updateState { copy(saveStatus = SaveStatus.FAILURE) }
            }
        }
    }

    private fun updateCharacterState(reducer: CreateCharacterUiState.() -> CreateCharacterUiState) {
        if (uiState.value.saveStatus == SaveStatus.SAVING || uiState.value.saveStatus == SaveStatus.SUCCESS) return
        val edited = uiState.value.reducer()
        val next = edited.copy(errors = validator.validate(edited.toDraft()).errors, saveStatus = SaveStatus.EDITING)
        savedStateHandle[KEY_SELECTED_TYPE] = next.selectedType
        savedStateHandle[KEY_NAME] = next.name
        savedStateHandle[KEY_PERSONALITY] = next.personality
        savedStateHandle[KEY_APPEARANCE] = next.appearance
        updateState { next }
    }
}

private fun SavedStateHandle.toUiState() =
    CreateCharacterUiState(
        selectedType = this[KEY_SELECTED_TYPE] ?: "Child",
        name = this[KEY_NAME] ?: "Oliver",
        personality =
            this[KEY_PERSONALITY]
                ?: "Curious and brave, loves exploring mysterious places and asking ‘what’s over there?’",
        appearance =
            this[KEY_APPEARANCE]
                ?: "Five years old with curly brown hair, bright green eyes, a yellow hoodie and blue sneakers.",
    )

private fun CreateCharacterUiState.toDraft() =
    CharacterDraft(
        CharacterName(name),
        CharacterPersonality(personality),
        CharacterAppearance(appearance),
        CharacterType.entries.firstOrNull { it.name.equals(selectedType, ignoreCase = true) },
    )
