package com.dozy.api.contracts

import com.dozy.api.contracts.v1.ValidationReason
import com.dozy.api.contracts.v1.character.CharacterDto
import com.dozy.api.contracts.v1.place.PlaceResponse
import com.dozy.api.contracts.v1.story.GenerationFailure
import com.dozy.api.contracts.v1.story.GenerationField
import com.dozy.api.contracts.v1.story.GenerationRequestDto
import com.dozy.api.contracts.v1.story.GenerationStage
import com.dozy.api.contracts.v1.story.StoryListField
import com.dozy.api.contracts.v1.story.StoryPageDto
import com.dozy.api.contracts.v1.story.StorySummaryDto
import com.dozy.api.contracts.v1.story.toRequestBody
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

class StoryContractTest {
    @Test fun `library pages contain only summary metadata and explicit null terminal cursor`() {
        val page = StoryPageDto(listOf(StorySummaryDto("id", "Title", "https://example.test/cover", 2, 123)))
        val json = Json { encodeDefaults = true }
        val wire = json.encodeToString(page)
        assertEquals(page, json.decodeFromString<StoryPageDto>(wire))
        val fields = Json.parseToJsonElement(wire).jsonObject
        assertEquals(setOf("stories", "nextCursor"), fields.keys)
        assertEquals(setOf("id", "title", "coverUrl", "readingMinutes", "createdAt"), fields.getValue("stories").jsonArray.single().jsonObject.keys)
        assertEquals(setOf(ValidationReason.INVALID_VALUE), StoryListField.LIMIT.reasons)
        assertEquals(StoryPageDto(emptyList()), Json.decodeFromString<StoryPageDto>("""{"stories":[],"nextCursor":null}"""))
    }

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
