package com.dozy.api.contracts

import com.dozy.api.contracts.v1.ApiErrorResponse
import com.dozy.api.contracts.v1.place.PlacesListError
import com.dozy.api.contracts.v1.place.PlacesSaveError
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

class PlaceValidationContractTest {
    @Test
    fun `every known business error round trips through the shared contract`() {
        val codes = listOf(
            "validation.name.required", "validation.name.too_long",
            "validation.description.required", "validation.description.too_long",
            "validation.inhabitants.too_long", "place.not_editable", "place.not_found", "place.conflict", "IDEMPOTENCY_CONFLICT"
        )
        assertEquals(codes, PlacesSaveError.known.map { it.code })
        assertEquals(codes.size, codes.toSet().size)
        PlacesSaveError.known.forEach { error ->
            val body = Json.encodeToString<PlacesSaveError>(error)
            assertEquals(error, Json.decodeFromString<PlacesSaveError>(body))
            assertEquals(error.code, Json.decodeFromString<ApiErrorResponse>(body).code)
        }
    }

    @Test
    fun `future codes deserialize and serialize without losing the original code`() {
        val code = "validation.new_field.new_rule"
        val body = """{"code":"$code"}"""
        assertEquals(PlacesSaveError.Unknown(code), Json.decodeFromString<PlacesSaveError>(body))
        assertEquals(PlacesListError.Unknown(code), Json.decodeFromString<PlacesListError>(body))
        assertEquals(body, Json.encodeToString<PlacesSaveError>(PlacesSaveError.Unknown(code)))
        assertEquals(body, Json.encodeToString<PlacesListError>(PlacesListError.Unknown(code)))
    }

    @Test
    fun `malformed envelopes fail decoding instead of becoming unknown business errors`() {
        for (body in listOf("<html>Error</html>", "{}", """{"code":null}""")) {
            assertFailsWith<SerializationException> { Json.decodeFromString<PlacesSaveError>(body) }
            assertFailsWith<SerializationException> { Json.decodeFromString<PlacesListError>(body) }
        }
    }
}
