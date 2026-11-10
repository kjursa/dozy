package com.dozy.mobile.feature.character.domain

data class SavedCharacter(
    val character: Character,
    val sync: CharacterSync,
)
