package com.dozy.features.character.application

import com.dozy.features.character.domain.model.Character
import com.dozy.features.character.domain.model.SaveCharacter
import com.dozy.features.character.domain.model.SaveCharacterResult
import com.dozy.features.character.domain.repository.CharacterRepository
import com.dozy.features.installation.domain.model.InstallationId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking

class CharacterServiceTest {
    @Test fun listsInStableNameOrderWithoutRevalidatingHistoricalValues() = runBlocking {
        val installation = InstallationId("owner")
        val historical = Character(Character.Id("old"), "x".repeat(70), "", "", Character.Type.CHILD, null)
        val characters = listOf(historical, historical.copy(id = Character.Id("b"), name = "Oliver"),
            historical.copy(id = Character.Id("a"), name = "oliver"))
        val repository = object : CharacterRepository {
            override suspend fun list(installation: InstallationId): List<Character> {
                assertEquals("owner", installation.value)
                return characters
            }
            override suspend fun save(installation: InstallationId, id: Character.Id, command: SaveCharacter): SaveCharacterResult = error("Unexpected save")
            override suspend fun delete(installation: InstallationId, id: Character.Id): Boolean = error("Unexpected delete")
        }
        val result = CharacterService(repository).list(installation)
        assertEquals(listOf("a", "b", "old"), result.map { it.id.value })
        assertEquals(historical, result.last())
    }
}
