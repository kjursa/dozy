package com.dozy.api.client.character.v1

import com.dozy.api.client.ApiResult
import com.dozy.api.contracts.v1.character.CharacterDto
import com.dozy.api.contracts.v1.character.SaveCharacterRequest

interface CharactersClient {
    suspend fun list(): ApiResult<List<CharacterDto>, CharactersListError>

    suspend fun save(id: String, request: SaveCharacterRequest): ApiResult<Unit, CharactersSaveError>
    suspend fun delete(id: String): ApiResult<Unit, CharactersDeleteError>
}
