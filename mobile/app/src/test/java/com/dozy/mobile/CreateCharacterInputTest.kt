package com.dozy.mobile

import com.dozy.mobile.feature.character.domain.model.*
import com.dozy.mobile.feature.character.domain.model.Character
import com.dozy.mobile.feature.character.domain.model.CreateCharacterErrors
import com.dozy.mobile.feature.character.domain.model.CreateCharacterInput
import com.dozy.mobile.feature.character.domain.model.CreateCharacterValidation
import org.junit.Assert.*
import org.junit.Test

class CreateCharacterInputTest {
    @Test fun validInputNormalizesAllText() {
        val input = (validate("  Finn  ", " Kind ", " Green eyes ") as CreateCharacterValidation.Valid).input
        assertEquals("Finn", input.name.value)
        assertEquals("Kind", input.personality.value)
        assertEquals("Green eyes", input.appearance.value)
        assertEquals(Character.Type.CHILD, input.type)
    }

    @Test fun nameRulesReturnOneTypedError() {
        val cases =
            listOf(
                " \n" to CreateCharacterErrors.Name.REQUIRED,
                "a".repeat(51) to CreateCharacterErrors.Name.TOO_LONG,
                "Captain\nFinn" to CreateCharacterErrors.Name.MUST_BE_SINGLE_LINE,
                "Captain\rFinn" to CreateCharacterErrors.Name.MUST_BE_SINGLE_LINE,
                "😀".repeat(26) to CreateCharacterErrors.Name.TOO_LONG,
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
            " " to CreateCharacterErrors.Description.REQUIRED,
            "a".repeat(2001) to CreateCharacterErrors.Description.TOO_LONG,
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
                CreateCharacterErrors.Name.REQUIRED,
                CreateCharacterErrors.Description.REQUIRED,
                CreateCharacterErrors.Description.REQUIRED,
                CreateCharacterErrors.Type.REQUIRED,
            ),
            errors,
        )
    }

    private fun validate(
        name: String = "Oliver",
        personality: String = "Kind",
        appearance: String = "Green eyes",
        type: Character.Type? = Character.Type.CHILD,
    ) = CreateCharacterInput.from(name, personality, appearance, type)
}
