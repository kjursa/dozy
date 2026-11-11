package com.dozy.mobile.feature.character.presentation.create

import com.dozy.mobile.feature.character.domain.model.Character

internal sealed interface CreateCharacterEffect {
    data object NavigateBack : CreateCharacterEffect

    data class Saved(
        val id: Character.Id,
    ) : CreateCharacterEffect
}
