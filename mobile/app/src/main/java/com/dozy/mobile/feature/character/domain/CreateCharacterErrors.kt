package com.dozy.mobile.feature.character.domain

data class CreateCharacterErrors(
    val name: CharacterNameError? = null,
    val personality: CharacterDescriptionError? = null,
    val appearance: CharacterDescriptionError? = null,
    val type: CharacterTypeError? = null,
) {
    internal val isEmpty: Boolean get() = name == null && personality == null && appearance == null && type == null
}
