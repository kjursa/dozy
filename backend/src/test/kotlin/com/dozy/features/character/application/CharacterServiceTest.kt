package com.dozy.features.character.application

import com.dozy.domain.id.CharacterId
import com.dozy.domain.id.IdempotencyKey
import com.dozy.domain.id.OwnerId
import com.dozy.features.character.domain.model.Character
import com.dozy.features.character.domain.model.SaveCharacter
import com.dozy.features.character.domain.model.SaveCharacterResult
import com.dozy.features.character.domain.repository.CharacterRepository
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class CharacterServiceTest {
    @Test fun listsInStableNameOrderWithoutRevalidatingHistoricalValues() = runBlocking {
        val owner = OwnerId.generate()
        val first = requireNotNull(CharacterId.parse("char-019a0000-0000-7000-8000-000000000001"))
        val second = requireNotNull(CharacterId.parse("char-019a0000-0000-7000-8000-000000000002"))
        val historical = Character(CharacterId.generate(), "x".repeat(70), "", "", Character.Type.CHILD, null)
        val characters = listOf(historical, historical.copy(id = second, name = "Oliver"),
            historical.copy(id = first, name = "oliver"))
        val repository = object : CharacterRepository {
            override suspend fun list(installation: OwnerId): List<Character> {
                assertEquals(owner, installation)
                return characters
            }
            override suspend fun create(
                installation: OwnerId,
                command: SaveCharacter,
                requestKey: IdempotencyKey?
            ): SaveCharacterResult = error("Unexpected creation")
            override suspend fun save(
                installation: OwnerId,
                id: CharacterId,
                command: SaveCharacter
            ): SaveCharacterResult = error("Unexpected save")
            override suspend fun delete(installation: OwnerId, id: CharacterId): Boolean =
                error("Unexpected delete")
        }
        val result = CharacterService(repository).list(owner)
        assertEquals(listOf(first, second, historical.id), result.map { it.id })
        assertEquals(historical, result.last())
    }
}
