package com.dozy.features.character.application

import com.dozy.features.character.domain.model.Character
import com.dozy.features.character.domain.model.SaveCharacter
import com.dozy.features.character.domain.repository.CharacterRepository
import com.dozy.features.installation.domain.model.InstallationId

class CharacterService(private val repository: CharacterRepository) {
    suspend fun list(installation: InstallationId) = repository.list(installation)
        .sortedWith(compareBy<Character> { it.name.lowercase() }.thenBy { it.id.value })
    suspend fun save(installation: InstallationId, id: Character.Id, command: SaveCharacter) =
        repository.save(installation, id, command)
    suspend fun delete(installation: InstallationId, id: Character.Id) = repository.delete(installation, id)
}
