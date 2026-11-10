package com.dozy.mobile.feature.character.presentation.create

import androidx.lifecycle.SavedStateHandle
import com.dozy.mobile.R
import com.dozy.mobile.feature.character.domain.CharacterDescriptionError
import com.dozy.mobile.feature.character.domain.CharacterNameError
import com.dozy.mobile.feature.character.domain.CharacterType
import com.dozy.mobile.feature.character.domain.CharacterTypeError
import com.dozy.mobile.feature.character.domain.CreateCharacterErrors
import com.dozy.mobile.feature.character.domain.CreateCharacterInput
import com.dozy.mobile.feature.character.domain.CreateCharacterValidation
import com.kjursa.foundation.presentation.text.UiText

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

/** Validate creation rules while retaining raw form text for editing. */
internal fun CreateCharacterUiState.validateInput(): CreateCharacterValidation =
    CreateCharacterInput.from(
        name = name,
        personality = personality,
        appearance = appearance,
        type = CharacterType.entries.firstOrNull { it.name.equals(selectedType, ignoreCase = true) },
    )

internal fun CreateCharacterUiState.withErrors(errors: CreateCharacterErrors = CreateCharacterErrors()) =
    copy(
        nameError = errors.name?.toUiText(),
        personalityError = errors.personality?.toUiText(),
        appearanceError = errors.appearance?.toUiText(),
        typeError = errors.type?.toUiText(),
    )

private fun CharacterNameError.toUiText(): UiText =
    when (this) {
        CharacterNameError.REQUIRED -> UiText.Resource(R.string.character_name_required)
        CharacterNameError.TOO_LONG -> UiText.Resource(R.string.character_name_too_long, listOf(CreateCharacterInput.NAME_MAX_LENGTH))
        CharacterNameError.MUST_BE_SINGLE_LINE -> UiText.Resource(R.string.character_name_single_line)
    }

private fun CharacterDescriptionError.toUiText(): UiText =
    when (this) {
        CharacterDescriptionError.REQUIRED -> UiText.Resource(R.string.character_description_required)
        CharacterDescriptionError.TOO_LONG ->
            UiText.Resource(
                R.string.character_description_too_long,
                listOf(CreateCharacterInput.DESCRIPTION_MAX_LENGTH),
            )
    }

private fun CharacterTypeError.toUiText(): UiText =
    when (this) {
        CharacterTypeError.REQUIRED -> UiText.Resource(R.string.character_type_required)
    }

internal fun CreateCharacterUiState.saveTo(handle: SavedStateHandle) {
    handle[KEY_SELECTED_TYPE] = selectedType
    handle[KEY_NAME] = name
    handle[KEY_PERSONALITY] = personality
    handle[KEY_APPEARANCE] = appearance
}
