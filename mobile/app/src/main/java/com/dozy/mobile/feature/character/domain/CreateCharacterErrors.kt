package com.dozy.mobile.feature.character.domain

data class CreateCharacterErrors(
    val name: Name? = null,
    val personality: Description? = null,
    val appearance: Description? = null,
    val type: Type? = null,
) {
    internal val isEmpty: Boolean get() = name == null && personality == null && appearance == null && type == null

    enum class Name { REQUIRED, TOO_LONG, MUST_BE_SINGLE_LINE }

    enum class Description { REQUIRED, TOO_LONG }

    enum class Type { REQUIRED }
}
