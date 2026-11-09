package com.dozy.mobile.data.character.api

import kotlinx.serialization.Serializable

/** Proposed wire shape. The current backend has no character routes; see CHARACTER_DATA.md. */
@Serializable
data class CharacterDto(
    val id: String,
    val name: String,
    val personality: String,
    val appearance: String,
    val type: String,
)
