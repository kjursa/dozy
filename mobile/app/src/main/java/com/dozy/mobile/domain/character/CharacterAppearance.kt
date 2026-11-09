package com.dozy.mobile.domain.character

@JvmInline value class CharacterAppearance private constructor(
    val value: String,
) {
    companion object {
        fun from(raw: String) = CharacterAppearance(validatedText(raw, 2000))
    }
}
