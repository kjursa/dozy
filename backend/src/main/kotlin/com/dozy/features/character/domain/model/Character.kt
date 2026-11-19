package com.dozy.features.character.domain.model

data class Character(
    val id: Id,
    val name: String,
    val personality: String,
    val appearance: String,
    val type: Type,
    val avatar: Avatar?,
) {
    @JvmInline value class Id(val value: String)
    enum class Type { CHILD, ANIMAL, FANTASY, OTHER }
    data class Avatar(val id: String, val url: String)
}

sealed interface SaveCharacterResult {
    data class Created(val id: Character.Id) : SaveCharacterResult
    data object NotFound : SaveCharacterResult
    data object Updated : SaveCharacterResult
    data object IdempotencyConflict : SaveCharacterResult
    data object Conflict : SaveCharacterResult
    data object ReferenceUnavailable : SaveCharacterResult
}
