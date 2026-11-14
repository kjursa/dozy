package com.dozy.api.contracts

import com.dozy.api.contracts.v1.place.PlacesResponse
import com.dozy.api.contracts.v1.place.SavePlaceRequest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

class PlaceContractTest {
    @Test
    fun `write contract has no identity or permission metadata`() {
        val body = Json.encodeToString(SavePlaceRequest("Garden", "Quiet", "Bees"))
        assertEquals(setOf("name", "description", "inhabitants"), Json.parseToJsonElement(body).jsonObject.keys)
    }

    @Test
    fun `v1 response preserves origin editability and optional inhabitants`() {
        val body = """{"places":[{"id":"builtin:woods","name":"Woods","description":"Quiet","origin":"SYSTEM","editable":false}]}"""
        val place = Json.decodeFromString<PlacesResponse>(body).places.single()
        assertEquals("SYSTEM", place.origin)
        assertEquals(false, place.editable)
        assertEquals(null, place.inhabitants)
        assertEquals("/api/v1", ApiVersion.V1.path)
    }
}
