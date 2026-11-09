package com.dozy.mobile.domain.character

@JvmInline value class CharacterName private constructor(
    val value: String,
) {
    companion object {
        fun from(raw: String): CharacterName {
            val value = validatedText(raw, 50)
            if ('\n' in value || '\r' in value) throw CharacterValueException(CharacterValueError.LINE_BREAK)
            return CharacterName(value)
        }
    }
}
