package com.dozy.mobile

import com.dozy.mobile.feature.character.domain.Character
import com.dozy.mobile.feature.character.domain.CharacterIdGenerator
import com.dozy.mobile.feature.character.domain.CharacterRepository
import com.dozy.mobile.feature.character.domain.ObserveCharacters
import com.dozy.mobile.feature.character.domain.SaveCharacter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class CharacterDomainTest {
    @Test fun savesNormalizedInputAndLoadsSavedCharacters() =
        runTest {
            val repository = FakeCharacterRepository()
            val result = save(repository)(createInput("  Finn  ")) as SaveCharacter.Result.Success
            assertEquals("Finn", result.saved.character.details.name.value)
            assertEquals(Character.Id("new"), result.saved.character.id)
            assertEquals(result.saved.character, ObserveCharacters(repository)().first().characters.last())
        }

    @Test fun exposesRepositoryFailure() =
        runTest {
            val repository = FakeCharacterRepository().apply { failure = IllegalStateException("disk full") }
            assertEquals(SaveCharacter.Result.Failure, save(repository)(createInput()))
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

    private fun save(repository: CharacterRepository) = SaveCharacter(repository, CharacterIdGenerator { Character.Id("new") })
}
