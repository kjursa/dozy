package com.dozy.mobile.feature.character.data.api

fun interface CharacterApi {
    /** Null means the API is disabled. An empty list is an authoritative snapshot. */
    suspend fun list(): List<CharacterDto>? = null

    suspend fun generateAvatar(input: AvatarRequestDto): AvatarDto? = null

    /** True only when the server acknowledges persistence. False means local-only. */
    suspend fun save(character: CharacterDto): Boolean
}
