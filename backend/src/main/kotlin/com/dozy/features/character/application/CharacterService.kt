package com.dozy.features.character.application

import com.dozy.domain.id.CharacterId
import com.dozy.domain.id.IdempotencyKey
import com.dozy.domain.id.OwnerId
import com.dozy.features.character.domain.model.Character
import com.dozy.features.character.domain.model.SaveCharacter
import com.dozy.features.character.domain.repository.CharacterRepository

class CharacterService(private val repository: CharacterRepository) {
    suspend fun list(ownerId: OwnerId) = repository.list(ownerId)
        .sortedWith(compareBy<Character> { it.name.lowercase() }.thenBy { it.id.value })
    suspend fun list(ownerId: OwnerId, isCurrent: (String) -> Boolean) = repository.list(ownerId, isCurrent)

    suspend fun create(
        ownerId: OwnerId,
        command: SaveCharacter,
        requestKey: IdempotencyKey? = null
    ) = repository.create(ownerId, command, requestKey)

    suspend fun save(ownerId: OwnerId, id: CharacterId, command: SaveCharacter) =
        repository.save(ownerId, id, command)
    suspend fun delete(ownerId: OwnerId, id: CharacterId) = repository.delete(ownerId, id)
}
