package com.dozy.api.contracts

import com.dozy.api.contracts.v1.character.CharacterDto
import com.dozy.api.contracts.v1.place.PlaceResponse
import com.dozy.api.contracts.v1.story.GenerationRequestDto
import com.dozy.api.contracts.v1.story.GenerationStage
import com.dozy.api.contracts.v1.story.GenerationFailure
import com.dozy.api.contracts.v1.story.GenerationField
import com.dozy.api.contracts.v1.story.toRequestBody
import com.dozy.api.contracts.v1.ValidationReason
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.serialization.json.Json

class StoryContractTest {
    @Test fun `snapshots retain historical text and automatic serialization omits manual draft data`() {
        val hero = CharacterDto("hero", "L".repeat(300), "K".repeat(3000), "Coat", "ANIMAL")
        val request = GenerationRequestDto(listOf(hero), "MANUAL", "SHORT", PlaceResponse("bundled:moon", "Moon", "Quiet", "SYSTEM", false), "Idea", "Ending")
        assertEquals(request, Json.decodeFromString(Json.encodeToString(request)))
        assertNull(request.cast.single().avatar)
        assertEquals(setOf("cast", "mode", "length"), request.copy(mode = "AUTOMATIC").toRequestBody().keys)
        assertEquals(setOf("QUEUED", "CREATING_TEXT", "CREATING_COVER", "READY", "FAILED"), GenerationStage.entries.map { it.name }.toSet())
        assertEquals(setOf("CONTENT_REJECTED", "GENERATION_FAILED"), GenerationFailure.entries.map { it.name }.toSet())
        assertEquals(setOf(ValidationReason.TOO_LONG), GenerationField.IDEA.reasons)
        assertEquals(setOf(ValidationReason.TOO_LONG), GenerationField.ENDING.reasons)
    }
}
