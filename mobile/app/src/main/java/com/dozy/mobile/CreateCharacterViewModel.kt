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

internal enum class SaveStatus { EDITING, SAVING, FAILURE }

internal data class CreateCharacterUiState(
    val errors: Map<CharacterField, String> = emptyMap(),
    val saveStatus: SaveStatus = SaveStatus.EDITING,
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

private data class CharacterFormValidation(
    val draft: CharacterDraft?,
    val errors: Map<CharacterField, String>,
)

/** Build domain values before saving; invalid text never reaches the use case. */
private fun CreateCharacterUiState.validateDraft(): CharacterFormValidation {
    val errors = mutableMapOf<CharacterField, String>()
    val validName = errors.validate(CharacterField.NAME, 50) { CharacterName.from(name) }
    val validPersonality = errors.validate(CharacterField.PERSONALITY, 2000) { CharacterPersonality.from(personality) }
    val validAppearance = errors.validate(CharacterField.APPEARANCE, 2000) { CharacterAppearance.from(appearance) }
    val type = CharacterType.entries.firstOrNull { it.name.equals(selectedType, ignoreCase = true) }
    if (type == null) errors[CharacterField.TYPE] = "Choose a character type."
    val draft =
        if (validName != null && validPersonality != null && validAppearance != null && type != null) {
            CharacterDraft(validName, validPersonality, validAppearance, type)
        } else {
            null
        }
    return CharacterFormValidation(draft, errors)
}

private inline fun <T> MutableMap<CharacterField, String>.validate(
    field: CharacterField,
    limit: Int,
    create: () -> T,
): T? =
    try {
        create()
    } catch (invalid: CharacterValueException) {
        this[field] =
            when (invalid.reason) {
                CharacterValueError.EMPTY -> "This field is required."
                CharacterValueError.TOO_LONG -> "Use at most $limit characters."
                CharacterValueError.LINE_BREAK -> "Use a single line."
            }
        null
    }
