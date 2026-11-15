package com.dozy.api.contracts

import com.dozy.api.contracts.v1.ApiErrorResponse
import com.dozy.api.contracts.v1.place.PlaceErrorCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.serialization.json.Json

class PlaceValidationContractTest {
    @Test
    fun `flat place error codes are stable and unique`() {
        val values = listOf(
            "validation.name.required", "validation.name.too_long",
            "validation.description.required", "validation.description.too_long",
            "validation.inhabitants.too_long", "place.not_editable", "place.conflict",
        )
        assertEquals(values, PlaceErrorCode.entries.map { it.value })
        values.forEach { assertEquals(it, PlaceErrorCode.fromValue(it)?.value) }
        assertEquals(values.size, values.toSet().size)
    }

    @Test
    fun `future code stays decodable without field or reason metadata`() {
        val error = Json.decodeFromString<ApiErrorResponse>("""{"code":"validation.new_field.new_rule"}""")
        assertNull(PlaceErrorCode.fromValue(error.code))
        assertNull(error.field)
        assertNull(error.reason)
    }
}
