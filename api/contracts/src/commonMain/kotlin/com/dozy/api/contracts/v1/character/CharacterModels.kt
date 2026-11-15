package com.dozy.api.contracts.v1.character

import kotlinx.serialization.Serializable

@Serializable
data class AvatarDto(val id: String, val url: String)

@Serializable
data class CharacterDto(
    val id: String,
    val name: String,
    val personality: String,
    val appearance: String,
    val type: String,
    val avatar: AvatarDto? = null,
)

@Serializable
data class SaveCharacterRequest(
    val name: String,
    val personality: String,
    val appearance: String,
    val type: String,
    val avatar: AvatarDto? = null,
)

enum class CharacterType { CHILD, ANIMAL, FANTASY, OTHER }
