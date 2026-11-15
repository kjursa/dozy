package com.dozy.api.contracts

import com.dozy.api.contracts.v1.characteravatar.AvatarRequestDto
import com.dozy.api.contracts.v1.characteravatar.AvatarJobDto
import com.dozy.api.contracts.v1.characteravatar.AvatarJobState
import com.dozy.api.contracts.v1.characteravatar.AvatarFailureCode
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class AvatarContractTest {
    @Test fun requestExcludesNameAndFailureUsesError() {
        val json = Json { encodeDefaults = true }
        val request = json.parseToJsonElement(json.encodeToString(AvatarRequestDto("CHILD", "kind", "eyes"))).jsonObject
        assertEquals(setOf("type", "personality", "appearance"), request.keys)
        val failed = AvatarJobDto("job", AvatarJobState.FAILED.name, error = AvatarFailureCode.GENERATION_FAILED.name)
        val encoded = json.encodeToString(failed)
        assertEquals(failed, json.decodeFromString<AvatarJobDto>(encoded))
        assertFalse("errorCode" in json.parseToJsonElement(encoded).jsonObject)
        assertEquals(setOf("QUEUED", "RUNNING", "READY", "FAILED"), AvatarJobState.entries.map { it.name }.toSet())
    }
}
