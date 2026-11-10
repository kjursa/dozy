package com.dozy.mobile.feature.character.domain

data class CharacterDraft(
    val name: CharacterName,
    val personality: CharacterPersonality,
    val appearance: CharacterAppearance,
    val type: CharacterType,
)
