package com.dozy.mobile

import com.dozy.mobile.domain.character.CharacterId
import com.dozy.mobile.domain.character.CharacterIdGenerator
import com.dozy.mobile.domain.character.CharacterRepository
import com.dozy.mobile.domain.character.ObserveCharacters
import com.dozy.mobile.domain.character.SaveCharacter
import com.dozy.mobile.domain.character.SaveCharacterResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class CharacterDomainTest {
    @Test fun savesNormalizedInputAndLoadsSavedCharacters() =
        runTest {
            val repository = FakeCharacterRepository()
            val result = save(repository)(createInput("  Finn  ")) as SaveCharacterResult.Success
            assertEquals("Finn", result.saved.character.details.name.value)
            assertEquals(CharacterId("new"), result.saved.character.id)
            assertEquals(result.saved.character, ObserveCharacters(repository)().first().characters.last())
        }

    @Test fun exposesRepositoryFailure() =
        runTest {
            val repository = FakeCharacterRepository().apply { failure = IllegalStateException("disk full") }
            assertEquals(SaveCharacterResult.Failure, save(repository)(createInput()))
        }

    @Test fun preservesCancellation() =
        runTest {
            val repository = FakeCharacterRepository().apply { failure = CancellationException("cancelled") }
            try {
                save(repository)(createInput())
                fail("Expected cancellation")
            } catch (_: CancellationException) {
            }
        }

    private fun save(repository: CharacterRepository) = SaveCharacter(repository, CharacterIdGenerator { CharacterId("new") })
}
