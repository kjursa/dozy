package com.dozy.mobile

import com.dozy.mobile.domain.character.CharacterAppearance
import com.dozy.mobile.domain.character.CharacterId
import com.dozy.mobile.domain.character.CharacterIdGenerator
import com.dozy.mobile.domain.character.CharacterName
import com.dozy.mobile.domain.character.CharacterPersonality
import com.dozy.mobile.domain.character.CharacterRepository
import com.dozy.mobile.domain.character.CharacterValueError
import com.dozy.mobile.domain.character.CharacterValueException
import com.dozy.mobile.domain.character.ObserveCharacters
import com.dozy.mobile.domain.character.SaveCharacter
import com.dozy.mobile.domain.character.SaveCharacterResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class CharacterDomainTest {
    @Test fun nameFactoryNormalizesAndChecksBoundaries() {
        assertEquals("Finn", CharacterName.from("  Finn  ").value)
        assertEquals(50, CharacterName.from("a".repeat(50)).value.length)
        assertEquals("Śpiący smok 🐉", CharacterName.from("Śpiący smok 🐉").value)
        assertInvalid(CharacterValueError.EMPTY) { CharacterName.from(" \n") }
        assertInvalid(CharacterValueError.TOO_LONG) { CharacterName.from("a".repeat(51)) }
        assertInvalid(CharacterValueError.LINE_BREAK) { CharacterName.from("Captain\nFinn") }
        assertInvalid(CharacterValueError.LINE_BREAK) { CharacterName.from("Captain\rFinn") }
        // The product limit intentionally uses String.length, including surrogate pairs.
        assertEquals(50, CharacterName.from("😀".repeat(25)).value.length)
        assertInvalid(CharacterValueError.TOO_LONG) { CharacterName.from("😀".repeat(26)) }
    }

    @Test fun descriptiveValuesValidateAndNormalizeAtConstruction() {
        assertEquals("Kind", CharacterPersonality.from("  Kind  ").value)
        assertEquals("Green eyes", CharacterAppearance.from(" Green eyes ").value)
        assertEquals(2000, CharacterPersonality.from("a".repeat(2000)).value.length)
        assertEquals(2000, CharacterAppearance.from("a".repeat(2000)).value.length)
        assertInvalid(CharacterValueError.EMPTY) { CharacterPersonality.from(" ") }
        assertInvalid(CharacterValueError.EMPTY) { CharacterAppearance.from("") }
        assertInvalid(CharacterValueError.TOO_LONG) { CharacterPersonality.from("a".repeat(2001)) }
        assertInvalid(CharacterValueError.TOO_LONG) { CharacterAppearance.from("a".repeat(2001)) }
    }

    private fun assertInvalid(
        reason: CharacterValueError,
        create: () -> Any,
    ) {
        try {
            create()
            fail("Expected $reason")
        } catch (invalid: CharacterValueException) {
            assertEquals(reason, invalid.reason)
        }
    }

    @Test fun savesNormalizedInputAndLoadsSavedCharacters() =
        runTest {
            val repository = FakeCharacterRepository()
            val result = save(repository)(validDraft.copy(name = CharacterName.from("  Finn  "))) as SaveCharacterResult.Success
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

    private fun save(repository: CharacterRepository) = SaveCharacter(repository, CharacterIdGenerator { CharacterId("new") })
}
