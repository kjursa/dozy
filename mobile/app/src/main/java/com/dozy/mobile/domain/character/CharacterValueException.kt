package com.dozy.mobile.domain.character

class CharacterValueException(
    val reason: CharacterValueError,
) : IllegalArgumentException(reason.name)
