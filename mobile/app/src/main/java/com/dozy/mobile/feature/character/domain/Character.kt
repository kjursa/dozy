package com.dozy.mobile.feature.character.domain

/** An existing character. Creation rules are enforced by CreateCharacterInput. */
data class Character(
    val id: Id,
    val details: Details,
) {
    @JvmInline value class Id(
        val value: String,
    )

    @JvmInline value class Name(
        val value: String,
    )

    @JvmInline value class Personality(
        val value: String,
    )

    @JvmInline value class Appearance(
        val value: String,
    )

    enum class Type { CHILD, ANIMAL, FANTASY, OTHER }

    data class Details(
        val name: Name,
        val personality: Personality,
        val appearance: Appearance,
        val type: Type,
    )
}
