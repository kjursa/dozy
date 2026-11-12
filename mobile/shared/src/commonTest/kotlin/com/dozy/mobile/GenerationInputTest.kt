package com.dozy.mobile

import com.dozy.mobile.feature.story.domain.model.*
import kotlin.test.*

class GenerationInputTest {
    private fun input() =
        (GenerationInput.from(listOf("1", "1"), false, 0, "custom", "  A star  ", "Home") as GenerationInput.Validation.Valid).input

    @Test fun validationCollectsErrorsAndAutomaticDiscardsAllHiddenFields() {
        val invalid =
            GenerationInput.from(
                emptyList(),
                false,
                7,
                null,
                "x".repeat(1001),
                "x".repeat(501),
            ) as GenerationInput.Validation.Invalid
        assertNotNull(invalid.errors.cast)
        assertNotNull(invalid.errors.length)
        assertNotNull(invalid.errors.idea)
        assertNotNull(invalid.errors.ending)
        val manual = input()
        assertEquals("A star", manual.idea)
        assertEquals(1, manual.cast.size)
        val automatic =
            (
                GenerationInput.from(
                    listOf("1"),
                    true,
                    1,
                    "custom",
                    "x".repeat(2000),
                    "ending",
                ) as GenerationInput.Validation.Valid
            ).input
        assertNull(automatic.place)
        assertNull(automatic.idea)
        assertNull(automatic.ending)
    }
}
