package com.dozy.api.client.character.v1

import com.dozy.api.contracts.v1.character.SaveCharacterRequest

interface CharactersClient {
    suspend fun list(): CharactersListResult

    suspend fun save(id: String, request: SaveCharacterRequest): CharactersSaveResult
    suspend fun delete(id: String): CharactersDeleteResult
}
