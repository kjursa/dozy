package com.dozy.mobile.presentation.createcharacter

import androidx.lifecycle.SavedStateHandle
import com.dozy.mobile.domain.character.CharacterAppearance
import com.dozy.mobile.domain.character.CharacterDraft
import com.dozy.mobile.domain.character.CharacterName
import com.dozy.mobile.domain.character.CharacterPersonality
import com.dozy.mobile.domain.character.CharacterType
import com.dozy.mobile.domain.character.CharacterValueError
import com.dozy.mobile.domain.character.CharacterValueException

private const val KEY_SELECTED_TYPE = "createCharacter.selectedType"
private const val KEY_NAME = "createCharacter.name"
private const val KEY_PERSONALITY = "createCharacter.personality"
private const val KEY_APPEARANCE = "createCharacter.appearance"

internal fun SavedStateHandle.toUiState() =
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

internal data class CharacterFormValidation(
    val draft: CharacterDraft?,
    val errors: Map<CharacterField, String>,
)

/** Build domain values before saving; invalid text never reaches the use case. */
internal fun CreateCharacterUiState.validateDraft(): CharacterFormValidation {
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

internal fun CreateCharacterUiState.saveTo(handle: SavedStateHandle) {
    handle[KEY_SELECTED_TYPE] = selectedType
    handle[KEY_NAME] = name
    handle[KEY_PERSONALITY] = personality
    handle[KEY_APPEARANCE] = appearance
}
