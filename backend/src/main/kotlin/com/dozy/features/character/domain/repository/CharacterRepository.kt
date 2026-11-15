package com.dozy.features.character.domain.repository

import com.dozy.features.character.domain.model.Character
import com.dozy.features.character.domain.model.SaveCharacter
import com.dozy.features.character.domain.model.SaveCharacterResult
import com.dozy.features.installation.domain.model.InstallationId

interface CharacterRepository {
    suspend fun list(installation: InstallationId): List<Character>
    suspend fun save(installation: InstallationId, id: Character.Id, command: SaveCharacter): SaveCharacterResult
    suspend fun delete(installation: InstallationId, id: Character.Id): Boolean
}
