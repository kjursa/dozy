package com.dozy.mobile.feature.character.presentation.create

import com.dozy.mobile.feature.character.domain.model.Character
import com.dozy.mobile.feature.character.domain.model.CreateCharacterErrors
import com.dozy.mobile.feature.character.domain.model.CreateCharacterInput
import com.dozy.mobile.feature.character.domain.model.CreateCharacterValidation
import com.dozy.mobile.resources.*
import com.kjursa.foundation.presentation.text.UiText

/** Validate creation rules while retaining raw form text for editing. */
fun CreateCharacterUiState.validateInput(): CreateCharacterValidation =
    CreateCharacterInput.from(
        name = name.orEmpty(),
        personality = personality.orEmpty(),
        appearance = appearance.orEmpty(),
        type = Character.Type.entries.firstOrNull { it.name.equals(selectedType, ignoreCase = true) },
    )

fun CreateCharacterUiState.withValidationErrors(errors: CreateCharacterErrors) =
    withErrors(
        name = errors.name?.toUiText(),
        personality = errors.personality?.toUiText(),
        appearance = errors.appearance?.toUiText(),
        type = errors.type?.toUiText(),
    )

private fun CreateCharacterErrors.Name.toUiText(): UiText =
    when (this) {
        CreateCharacterErrors.Name.REQUIRED -> UiText.Resource(Res.string.character_name_required)
        CreateCharacterErrors.Name.TOO_LONG ->
            UiText.Resource(
                Res.string.character_name_too_long,
                listOf(CreateCharacterInput.NAME_MAX_LENGTH),
            )
        CreateCharacterErrors.Name.MUST_BE_SINGLE_LINE -> UiText.Resource(Res.string.character_name_single_line)
    }

private fun CreateCharacterErrors.Description.toUiText(): UiText =
    when (this) {
        CreateCharacterErrors.Description.REQUIRED -> UiText.Resource(Res.string.character_description_required)
        CreateCharacterErrors.Description.TOO_LONG ->
            UiText.Resource(
                Res.string.character_description_too_long,
                listOf(CreateCharacterInput.DESCRIPTION_MAX_LENGTH),
            )
    }

private fun CreateCharacterErrors.Type.toUiText(): UiText =
    when (this) {
        CreateCharacterErrors.Type.REQUIRED -> UiText.Resource(Res.string.character_type_required)
    }

fun CreateCharacterUiState.withCharacter(character: Character): CreateCharacterUiState {
    val loaded =
        copy(
            loading = false,
            editingId = character.id.value,
            name = character.details.name.value,
            personality = character.details.personality.value,
            appearance = character.details.appearance.value,
            selectedType =
                character.details.type.name
                    .lowercase()
                    .replaceFirstChar { it.uppercase() },
            avatarId = character.avatar?.id?.value,
            avatarUrl = character.avatar?.url?.value,
        )
    return loaded.copy(avatarDescription = loaded.descriptionKey)
}
