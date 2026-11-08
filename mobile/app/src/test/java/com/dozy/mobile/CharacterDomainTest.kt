package com.dozy.mobile

import com.dozy.mobile.domain.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class CharacterDomainTest {
    private val validator = CharacterValidator()

    @Test fun validatesEveryFieldAndLengthBoundary() {
        assertTrue(validator.validate(validDraft).isValid)
        val cases =
            listOf(
                validDraft.copy(name = CharacterName(" \n")) to CharacterField.NAME,
                validDraft.copy(name = CharacterName("a".repeat(81))) to CharacterField.NAME,
                validDraft.copy(personality = CharacterPersonality(" ")) to CharacterField.PERSONALITY,
                validDraft.copy(personality = CharacterPersonality("a".repeat(2001))) to CharacterField.PERSONALITY,
                validDraft.copy(appearance = CharacterAppearance("")) to CharacterField.APPEARANCE,
                validDraft.copy(appearance = CharacterAppearance("a".repeat(2001))) to CharacterField.APPEARANCE,
                validDraft.copy(type = null) to CharacterField.TYPE,
            )
        cases.forEach { (draft, field) -> assertEquals(setOf(field), validator.validate(draft).errors.keys) }
        assertTrue(
            validator
                .validate(
                    validDraft.copy(
                        name = CharacterName("a".repeat(80)),
                        personality = CharacterPersonality("a".repeat(2000)),
                        appearance = CharacterAppearance("a".repeat(2000)),
                    ),
                ).isValid,
        )
    }

    @Test fun invalidInputNeverReachesPersistence() =
        runTest {
            val repository = FakeCharacterRepository()
            val result = save(repository)(validDraft.copy(name = CharacterName("")))
            assertTrue(result is SaveCharacterResult.Invalid)
            assertEquals(0, repository.saves)
        }

    @Test fun savesNormalizedInputAndLoadsSavedCharacters() =
        runTest {
            val repository = FakeCharacterRepository()
            val result = save(repository)(validDraft.copy(name = CharacterName("  Finn  "))) as SaveCharacterResult.Success
            assertEquals("Finn", result.saved.character.details.name.value)
            assertEquals(CharacterId("new"), result.saved.character.id)
            assertEquals(result.saved.character, ObserveCharacters(repository)().first().characters.last())
        }

    @Test fun exposesRepositoryFailure() =
        runTest {
            val repository = FakeCharacterRepository().apply { failure = IllegalStateException("disk full") }
            assertEquals(SaveCharacterResult.Failure, save(repository)(validDraft))
        }

    @Test fun preservesCancellation() =
        runTest {
            val repository = FakeCharacterRepository().apply { failure = CancellationException("cancelled") }
            try {
                save(repository)(validDraft)
                fail("Expected cancellation")
            } catch (_: CancellationException) {
            }
        }

    private fun save(repository: CharacterRepository) = SaveCharacter(repository, validator, CharacterIdGenerator { CharacterId("new") })
}
