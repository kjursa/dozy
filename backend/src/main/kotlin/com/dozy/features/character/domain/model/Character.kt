package com.dozy.features.character.domain.model

import com.dozy.domain.id.AssetId
import com.dozy.domain.id.CharacterId

data class Character(
    val id: CharacterId,
    val name: String,
    val personality: String,
    val appearance: String,
    val type: Type,
    val avatar: Avatar?,
) {
    enum class Type { CHILD, ANIMAL, FANTASY, OTHER }
    data class Avatar(val id: AssetId, val url: String)
}

sealed interface SaveCharacterResult {
    data class Created(val id: CharacterId) : SaveCharacterResult
    data object NotFound : SaveCharacterResult
    data object Updated : SaveCharacterResult
    data object IdempotencyConflict : SaveCharacterResult
    data object Conflict : SaveCharacterResult
    data object ReferenceUnavailable : SaveCharacterResult
}
