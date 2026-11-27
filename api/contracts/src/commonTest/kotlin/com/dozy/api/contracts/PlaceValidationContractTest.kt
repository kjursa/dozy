package com.dozy.api.contracts

import com.dozy.api.contracts.v1.ApiErrorResponse
import com.dozy.api.contracts.v1.ValidationReason
import com.dozy.api.contracts.v1.place.PlaceField
import com.dozy.api.contracts.v1.place.PlacesListError
import com.dozy.api.contracts.v1.place.PlacesSaveError
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PlaceValidationContractTest {
    @Test
    fun `every known business error round trips through the shared contract`() {
        val codes =
            listOf(
                "place.not_editable",
                "place.not_found",
                "place.conflict",
                "IDEMPOTENCY_CONFLICT",
                "place.invalid.field",
                "place.invalid.field",
                "place.invalid.field",
                "place.invalid.field",
                "place.invalid.field",
            )
        assertEquals(codes.toSet(), PlacesSaveError.known.map { it.code }.toSet())
        PlacesSaveError.known.forEach { error ->
            val body = Json.encodeToString<PlacesSaveError>(error)
            assertEquals(error, Json.decodeFromString<PlacesSaveError>(body))
            assertEquals(error.code, Json.decodeFromString<ApiErrorResponse>(body).code)
        }
    }

    @Test
    fun `validation errors carry field and reason and unknown reasons fall back to unknown`() {
        val error = PlacesSaveError.invalidField(PlaceField.NAME, ValidationReason.REQUIRED)
        val body = Json.encodeToString<PlacesSaveError>(error)
        assertEquals("""{"code":"place.invalid.field","field":"name","reason":"required"}""", body)
        assertEquals(error, Json.decodeFromString<PlacesSaveError>(body))
        assertEquals(
            PlacesSaveError.Unknown("place.invalid.field"),
            Json.decodeFromString<PlacesSaveError>("""{"code":"place.invalid.field","field":"name","reason":"new_reason"}"""),
        )
        val tolerantJson = Json { ignoreUnknownKeys = true }
        assertEquals(
            error,
            tolerantJson.decodeFromString<PlacesSaveError>(
                """{"code":"place.invalid.field","field":"name","reason":"required","futureField":"ignored"}""",
            ),
        )
        assertEquals(
            PlacesSaveError.Unknown("place.invalid.field"),
            Json.decodeFromString<PlacesSaveError>("""{"code":"place.invalid.field","reason":"required"}"""),
        )
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
