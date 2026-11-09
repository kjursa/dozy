package com.dozy.mobile.domain.character

internal fun validatedText(
    raw: String,
    limit: Int,
): String {
    val value = raw.trim()
    if (value.isEmpty()) throw CharacterValueException(CharacterValueError.EMPTY)
    if (value.length > limit) throw CharacterValueException(CharacterValueError.TOO_LONG)
    return value
}
