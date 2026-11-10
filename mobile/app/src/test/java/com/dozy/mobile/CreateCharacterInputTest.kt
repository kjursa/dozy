package com.dozy.mobile

import com.dozy.mobile.domain.character.*
import org.junit.Assert.*
import org.junit.Test

class CreateCharacterInputTest {
    @Test fun validInputNormalizesAllText() {
        val input = (validate("  Finn  ", " Kind ", " Green eyes ") as CreateCharacterValidation.Valid).input
        assertEquals("Finn", input.name.value)
        assertEquals("Kind", input.personality.value)
        assertEquals("Green eyes", input.appearance.value)
        assertEquals(CharacterType.CHILD, input.type)
    }

    @Test fun nameRulesReturnOneTypedError() {
        val cases =
            listOf(
                " \n" to CharacterNameError.REQUIRED,
                "a".repeat(51) to CharacterNameError.TOO_LONG,
                "Captain\nFinn" to CharacterNameError.MUST_BE_SINGLE_LINE,
                "Captain\rFinn" to CharacterNameError.MUST_BE_SINGLE_LINE,
                "😀".repeat(26) to CharacterNameError.TOO_LONG,
            )
        for ((name, expected) in cases) {
            val errors = (validate(name) as CreateCharacterValidation.Invalid).errors
            assertEquals(expected, errors.name)
            assertNull(errors.personality)
            assertNull(errors.appearance)
            assertNull(errors.type)
        }
    }

    @Test fun nameLengthUsesStringLengthAndAllowsUnicode() {
        for (name in listOf("a", "a".repeat(50), "😀".repeat(25), "Śpiący smok 🐉")) {
            assertTrue(validate(name) is CreateCharacterValidation.Valid)
        }
    }

    @Test fun descriptionsRespectRequiredAndLengthRules() {
        assertTrue(validate(personality = "a".repeat(2000), appearance = "a".repeat(2000)) is CreateCharacterValidation.Valid)
        for ((text, expected) in listOf(
            " " to CharacterDescriptionError.REQUIRED,
            "a".repeat(2001) to CharacterDescriptionError.TOO_LONG,
        )) {
            val errors = (validate(personality = text, appearance = text) as CreateCharacterValidation.Invalid).errors
            assertEquals(expected, errors.personality)
            assertEquals(expected, errors.appearance)
            assertNull(errors.name)
            assertNull(errors.type)
        }
    }

    @Test fun reportsAllInvalidFieldsTogether() {
        val errors = (validate("", "", "", null) as CreateCharacterValidation.Invalid).errors
        assertEquals(
            CreateCharacterErrors(
                CharacterNameError.REQUIRED,
                CharacterDescriptionError.REQUIRED,
                CharacterDescriptionError.REQUIRED,
                CharacterTypeError.REQUIRED,
            ),
            errors,
        )
    }

    private fun validate(
        name: String = "Oliver",
        personality: String = "Kind",
        appearance: String = "Green eyes",
        type: CharacterType? = CharacterType.CHILD,
    ) = CreateCharacterInput.from(name, personality, appearance, type)
}
