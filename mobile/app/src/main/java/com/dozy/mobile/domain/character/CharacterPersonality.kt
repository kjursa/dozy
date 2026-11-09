package com.dozy.mobile.domain.character

@JvmInline value class CharacterPersonality private constructor(
    val value: String,
) {
    companion object {
        fun from(raw: String) = CharacterPersonality(validatedText(raw, 2000))
    }
}
