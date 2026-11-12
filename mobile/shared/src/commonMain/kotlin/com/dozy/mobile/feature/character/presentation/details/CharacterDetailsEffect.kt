package com.dozy.mobile.feature.character.presentation.details

import com.dozy.mobile.feature.character.domain.model.Character

sealed interface CharacterDetailsEffect {
    data object Back : CharacterDetailsEffect

    data object Deleted : CharacterDetailsEffect

    data class Edit(
        val id: Character.Id,
    ) : CharacterDetailsEffect

    data class Story(
        val id: Character.Id,
    ) : CharacterDetailsEffect
}
