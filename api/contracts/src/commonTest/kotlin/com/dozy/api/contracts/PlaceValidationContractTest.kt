package com.dozy.api.contracts

import com.dozy.api.contracts.v1.ApiErrorResponse
import com.dozy.api.contracts.v1.ValidationReason
import com.dozy.api.contracts.v1.place.PlaceField
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.serialization.json.Json

class PlaceValidationContractTest {
    @Test
    fun `field and reason wire values remain stable`() {
        val fields = mapOf(PlaceField.NAME to "name", PlaceField.DESCRIPTION to "description", PlaceField.INHABITANTS to "inhabitants")
        fields.forEach { (field, value) ->
            assertEquals(value, field.value)
            assertEquals(field, PlaceField.fromValue(value))
        }
        val reasons = mapOf(ValidationReason.REQUIRED to "REQUIRED", ValidationReason.TOO_LONG to "TOO_LONG")
        reasons.forEach { (reason, value) ->
            assertEquals(value, reason.value)
            assertEquals(reason, ValidationReason.fromValue(value))
        }
    }

    @Test
    fun `contract defines optional and required field error combinations`() {
        val required = setOf(ValidationReason.REQUIRED, ValidationReason.TOO_LONG)
        assertEquals(required, PlaceField.NAME.reasons)
        assertEquals(required, PlaceField.DESCRIPTION.reasons)
        assertEquals(setOf(ValidationReason.TOO_LONG), PlaceField.INHABITANTS.reasons)
    }

    @Test
    fun `future validation metadata remains decodable as strings`() {
        val error = Json.decodeFromString<ApiErrorResponse>(
            """{"code":"VALIDATION_FAILED","field":"newField","reason":"NEW_REASON"}""",
        )
        assertEquals("newField", error.field)
        assertEquals("NEW_REASON", error.reason)
        assertNull(PlaceField.fromValue(requireNotNull(error.field)))
        assertNull(ValidationReason.fromValue(requireNotNull(error.reason)))
    }
}
