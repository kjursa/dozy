package com.dozy.features.character.domain.repository

import com.dozy.domain.id.CharacterId
import com.dozy.domain.id.IdempotencyKey
import com.dozy.domain.id.OwnerId
import com.dozy.features.character.domain.model.Character
import com.dozy.features.character.domain.model.SaveCharacter
import com.dozy.features.character.domain.model.SaveCharacterResult

interface CharacterRepository {
    suspend fun list(installation: OwnerId): List<Character>
    suspend fun create(
        installation: OwnerId,
        command: SaveCharacter,
        requestKey: IdempotencyKey? = null
    ): SaveCharacterResult
    suspend fun save(
        installation: OwnerId,
        id: CharacterId,
        command: SaveCharacter
    ): SaveCharacterResult
    suspend fun delete(installation: OwnerId, id: CharacterId): Boolean
}
