package com.dozy.features.character.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class SaveCharacterTest {
    @Test fun normalizesWritesAndChecksUtf16LimitsAndAllTypes() {
        for (type in Character.Type.entries) {
            val parsed = assertIs<SaveCharacter.Result.Valid>(SaveCharacter.parse("  Oliver  ", " Curious ", " Green eyes ", type.name, null)).command
            assertEquals("Oliver", parsed.name)
            assertEquals("Curious", parsed.personality)
            assertEquals("Green eyes", parsed.appearance)
            assertEquals(type, parsed.type)
        }
        assertIs<SaveCharacter.Result.Valid>(SaveCharacter.parse("🌙".repeat(25), "p".repeat(2000), "a".repeat(2000), "CHILD", null))
        assertEquals(SaveCharacter.Result.Invalid(SaveCharacter.Field.NAME, SaveCharacter.Reason.TOO_LONG),
            SaveCharacter.parse("🌙".repeat(26), "p", "a", "CHILD", null))
    }

    @Test fun returnsTypedErrorsForEveryWriteRule() {
        for ((name, reason) in listOf(" " to SaveCharacter.Reason.REQUIRED, "n".repeat(51) to SaveCharacter.Reason.TOO_LONG,
            "a\nb" to SaveCharacter.Reason.SINGLE_LINE, "a\rb" to SaveCharacter.Reason.SINGLE_LINE,
            "a\u2028b" to SaveCharacter.Reason.SINGLE_LINE)) {
            assertEquals(SaveCharacter.Result.Invalid(SaveCharacter.Field.NAME, reason), SaveCharacter.parse(name, "p", "a", "CHILD", null))
        }
        for ((value, reason) in listOf(" " to SaveCharacter.Reason.REQUIRED, "x".repeat(2001) to SaveCharacter.Reason.TOO_LONG)) {
            assertEquals(SaveCharacter.Result.Invalid(SaveCharacter.Field.PERSONALITY, reason), SaveCharacter.parse("n", value, "a", "CHILD", null))
            assertEquals(SaveCharacter.Result.Invalid(SaveCharacter.Field.APPEARANCE, reason), SaveCharacter.parse("n", "p", value, "CHILD", null))
        }
        assertEquals(SaveCharacter.Result.Invalid(SaveCharacter.Field.TYPE, SaveCharacter.Reason.INVALID_VALUE),
            SaveCharacter.parse("n", "p", "a", "UNKNOWN", null))
    }
}
