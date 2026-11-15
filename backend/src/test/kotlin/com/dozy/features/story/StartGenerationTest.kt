package com.dozy.features.story

import com.dozy.features.story.domain.model.StartGeneration
import com.dozy.features.story.domain.model.StoryConfiguration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class StartGenerationTest {
    @Test fun `historical descriptions are preserved and only optional wishes have current length limits`() {
        val input = configuration()
        val historical = input.copy(cast = input.cast.map { it.copy(name = "L".repeat(200), personality = "K".repeat(3000)) }, place = input.place?.copy(name = "M".repeat(200), description = "D".repeat(2000)))
        val parsed = assertIs<StartGeneration.Result.Valid>(StartGeneration.parse(historical)).command.configuration
        assertEquals(historical.cast, parsed.cast)
        assertEquals(historical.place, parsed.place)
        assertEquals("Find a star", parsed.idea)
        assertEquals("Home", parsed.ending)
        assertIs<StartGeneration.Result.Valid>(StartGeneration.parse(input.copy(idea = "😀".repeat(500), ending = "😀".repeat(250))))
        assertEquals(StartGeneration.Result.Invalid(StartGeneration.Field.IDEA, StartGeneration.Reason.TOO_LONG), StartGeneration.parse(input.copy(idea = "😀".repeat(501))))
        assertEquals(StartGeneration.Result.Invalid(StartGeneration.Field.ENDING, StartGeneration.Reason.TOO_LONG), StartGeneration.parse(input.copy(ending = "😀".repeat(251))))
    }
    @Test fun `cast must be nonempty and source identifiers unique`() {
        val input = configuration()
        assertEquals(StartGeneration.Result.Invalid(StartGeneration.Field.CAST, StartGeneration.Reason.REQUIRED), StartGeneration.parse(input.copy(cast = emptyList())))
        assertEquals(StartGeneration.Result.Invalid(StartGeneration.Field.CAST, StartGeneration.Reason.INVALID_VALUE), StartGeneration.parse(input.copy(cast = input.cast + input.cast)))
    }
}
