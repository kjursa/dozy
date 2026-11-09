package com.dozy.mobile.domain.character

data class CharacterDraft(
    val name: CharacterName,
    val personality: CharacterPersonality,
    val appearance: CharacterAppearance,
    val type: CharacterType,
)
