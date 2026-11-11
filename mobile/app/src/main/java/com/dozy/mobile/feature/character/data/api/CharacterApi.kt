package com.dozy.mobile.feature.character.data.api

fun interface CharacterApi {
    suspend fun generateAvatar(input: AvatarRequestDto): AvatarDto? = null

    /** True only when the server acknowledges persistence. False means local-only. */
    suspend fun save(character: CharacterDto): Boolean
}
