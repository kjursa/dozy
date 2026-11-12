package com.dozy.mobile

import com.dozy.mobile.feature.character.domain.model.Character
import com.dozy.mobile.feature.character.domain.model.EditCharacterInput
import com.dozy.mobile.feature.character.domain.repository.CharacterRepository
import com.dozy.mobile.feature.character.domain.usecase.CharacterIdGenerator
import com.dozy.mobile.feature.character.domain.usecase.ObserveCharacters
import com.dozy.mobile.feature.character.domain.usecase.SaveCharacter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.*
import kotlin.test.Test

class CharacterDomainTest {
    @Test fun savesNormalizedInputAndLoadsSavedCharacters() =
        runTest {
            val repository = FakeCharacterRepository()
            val result = save(repository)(createInput("  Finn  "), avatar()) as SaveCharacter.Result.Success
            assertEquals("Finn", result.saved.character.details.name.value)
            assertEquals(Character.Id("new"), result.saved.character.id)
            assertEquals(result.saved.character, ObserveCharacters(repository)().first().characters.last())
        }

    @Test fun exposesRepositoryFailure() =
        runTest {
            val repository = FakeCharacterRepository().apply { failure = IllegalStateException("disk full") }
            assertEquals(SaveCharacter.Result.Failure, save(repository)(createInput(), avatar()))
        }

    @Test fun preservesCancellation() =
        runTest {
            val repository = FakeCharacterRepository().apply { failure = CancellationException("cancelled") }
            try {
                save(repository)(createInput(), avatar())
                fail("Expected cancellation")
            } catch (_: CancellationException) {
            }
        }

    @Test fun editingPreservesIdAndDoesNotAllocateOrDuplicate() =
        runTest {
            val repository = FakeCharacterRepository()
            val save = SaveCharacter(repository, CharacterIdGenerator { error("Editing cannot allocate an ID") })
            val result = save(EditCharacterInput.from(Character.Id("1"), createInput("Edited")), avatar()) as SaveCharacter.Result.Success
            assertEquals(Character.Id("1"), result.saved.character.id)
            assertEquals(3, repository.characters.value.size)
            assertEquals(
                "Edited",
                repository.characters.value
                    .single { it.id.value == "1" }
                    .details.name.value,
            )
            assertEquals(SaveCharacter.Result.Unavailable, save(EditCharacterInput.from(Character.Id("missing"), createInput()), avatar()))
            assertEquals(1, repository.saves)
            repository.failure = IllegalStateException("disk")
            assertEquals(SaveCharacter.Result.Failure, save(EditCharacterInput.from(Character.Id("1"), createInput()), avatar()))
            repository.failure = CancellationException("cancel")
            try {
                save(EditCharacterInput.from(Character.Id("1"), createInput()), avatar())
                fail("Expected cancellation")
            } catch (_: CancellationException) {
            }
        }

    private fun save(repository: CharacterRepository) = SaveCharacter(repository, CharacterIdGenerator { Character.Id("new") })
}
