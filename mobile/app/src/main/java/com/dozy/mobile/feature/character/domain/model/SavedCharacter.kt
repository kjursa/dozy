package com.dozy.mobile.feature.character.domain.model

data class SavedCharacter(
    val character: Character,
    val sync: CharacterSync,
)
