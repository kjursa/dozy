package com.dozy.api.client.character.v1

import com.dozy.api.contracts.v1.character.SaveCharacterRequest

interface CharactersClient {
    suspend fun list(ifNoneMatch: String? = null): CharactersListResult

    suspend fun create(
        request: SaveCharacterRequest,
        requestKey: String? = null
    ): CharactersCreateResult

    suspend fun save(id: String, request: SaveCharacterRequest): CharactersSaveResult
    suspend fun delete(id: String): CharactersDeleteResult
}
