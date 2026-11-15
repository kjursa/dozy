package com.dozy.mobile.feature.character.data.api

import com.dozy.api.contracts.v1.character.AvatarDto

/** Avatar generation boundary retained until API2. CRUD uses CharactersClients. */
fun interface CharacterApi {
    suspend fun generateAvatar(input: AvatarRequestDto): AvatarDto?
}
