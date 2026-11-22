package com.dozy.features.character.domain.repository

import com.dozy.domain.VersionedCollection
import com.dozy.domain.id.CharacterId
import com.dozy.domain.id.IdempotencyKey
import com.dozy.domain.id.OwnerId
import com.dozy.features.character.domain.model.Character
import com.dozy.features.character.domain.model.SaveCharacter
import com.dozy.features.character.domain.model.SaveCharacterResult

interface CharacterRepository {
    suspend fun list(ownerId: OwnerId): List<Character> = checkNotNull(list(ownerId) { false }.items)

    suspend fun list(
        ownerId: OwnerId,
        isCurrent: (String) -> Boolean,
    ): VersionedCollection<Character>
    suspend fun create(
        ownerId: OwnerId,
        command: SaveCharacter,
        requestKey: IdempotencyKey? = null
    ): SaveCharacterResult
    suspend fun save(
        ownerId: OwnerId,
        id: CharacterId,
        command: SaveCharacter
    ): SaveCharacterResult
    suspend fun delete(ownerId: OwnerId, id: CharacterId): Boolean
}
