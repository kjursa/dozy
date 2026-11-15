package com.dozy.api.contracts

import com.dozy.api.contracts.v1.ValidationReason
import com.dozy.api.contracts.v1.character.AvatarDto
import com.dozy.api.contracts.v1.character.CharacterDto
import com.dozy.api.contracts.v1.character.CharacterField
import com.dozy.api.contracts.v1.character.CharacterType
import com.dozy.api.contracts.v1.character.SaveCharacterRequest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CharacterContractTest {
    @Test fun minimalWriteAndBareArrayRead() {
        val body = """{"name":"Oliver","personality":"Curious","appearance":"Green eyes","type":"CHILD"}"""
        val request = Json.decodeFromString<SaveCharacterRequest>(body)
        assertNull(request.avatar)
        assertEquals(setOf("name", "personality", "appearance", "type"), Json.parseToJsonElement(Json.encodeToString(request)).jsonObject.keys)
        val list = Json.decodeFromString<List<CharacterDto>>("""[{"id":"one","name":"Historical","personality":"","appearance":"","type":"CHILD"}]""")
        assertNull(list.single().avatar)
        assertEquals("", list.single().personality)
        val avatar = AvatarDto("asset", "https://example.test/a.png")
        assertEquals(avatar, Json.decodeFromString<SaveCharacterRequest>(Json.encodeToString(request.copy(avatar = avatar))).avatar)
    }

    @Test fun ownsAllTypesAndAllowedFieldReasons() {
        assertEquals(setOf("CHILD", "ANIMAL", "FANTASY", "OTHER"), CharacterType.entries.map { it.name }.toSet())
        assertEquals(setOf(ValidationReason.REQUIRED, ValidationReason.TOO_LONG, ValidationReason.SINGLE_LINE), CharacterField.NAME.reasons)
        assertEquals(setOf(ValidationReason.INVALID_VALUE), CharacterField.TYPE.reasons)
        assertEquals(setOf(ValidationReason.REQUIRED, ValidationReason.TOO_LONG), CharacterField.PERSONALITY.reasons)
        assertEquals(CharacterField.PERSONALITY.reasons, CharacterField.APPEARANCE.reasons)
    }
}
