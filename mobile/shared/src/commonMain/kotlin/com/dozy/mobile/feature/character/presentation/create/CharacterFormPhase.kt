package com.dozy.mobile.feature.character.presentation.create

import com.dozy.mobile.feature.character.domain.model.CreateCharacterInput
import com.dozy.mobile.feature.character.domain.model.CreateCharacterValidation

internal sealed interface CharacterFormPhase {
    data object Unavailable : CharacterFormPhase

    data object Editing : CharacterFormPhase

    data class ReadyToGenerate(val input: CreateCharacterInput) : CharacterFormPhase {
        fun beginGeneration(state: CreateCharacterUiState) = state.copy(generating = true, generationError = null)
    }

    data class AvatarReady(val generation: ReadyToGenerate) : CharacterFormPhase

    data object Generating : CharacterFormPhase

    data object Saving : CharacterFormPhase
}

internal fun CreateCharacterUiState.formPhase(): CharacterFormPhase {
    if (loading || unavailable || loadError != null) return CharacterFormPhase.Unavailable
    if (generating) return CharacterFormPhase.Generating
    if (saveStatus == SaveStatus.SAVING) return CharacterFormPhase.Saving

    val validation = validateInput() as? CreateCharacterValidation.Valid ?: return CharacterFormPhase.Editing
    val ready = CharacterFormPhase.ReadyToGenerate(validation.input)
    return if (avatarId != null && avatarUrl != null) CharacterFormPhase.AvatarReady(ready) else ready
}
