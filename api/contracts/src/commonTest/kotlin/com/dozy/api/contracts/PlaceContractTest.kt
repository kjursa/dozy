package com.dozy.api.contracts

import com.dozy.api.contracts.v1.place.PlacesResponse
import com.dozy.api.contracts.v1.place.SavePlaceRequest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

class PlaceContractTest {
    @Test
    fun `write contract has no identity or permission metadata`() {
        val body = Json.encodeToString(SavePlaceRequest("Garden", "Quiet", "Bees"))
        assertEquals(setOf("name", "description", "inhabitants"), Json.parseToJsonElement(body).jsonObject.keys)
    }

    @Test
    fun `v1 response preserves origin editability and optional inhabitants`() {
        val body = """{"places":[{"id":"place-00000000-0000-7000-8000-000000000001","name":"Woods","description":"Quiet","origin":"SYSTEM","editable":false}]}"""
        val place = Json.decodeFromString<PlacesResponse>(body).places.single()
        assertEquals("SYSTEM", place.origin)
        assertEquals(false, place.editable)
        assertEquals(null, place.inhabitants)
        assertEquals(null, place.imageUrl)
        assertEquals("/api/v1", ApiVersion.V1.path)
    }

    @Test
    fun `v1 response reads the optional place thumbnail URL`() {
        val body = """{"places":[{"id":"place-00000000-0000-7000-8000-000000000001","name":"Woods","description":"Quiet","origin":"SYSTEM","editable":false,"imageUrl":"https://example.test/api/v1/place-icons/forest.png"}]}"""
        val place = Json.decodeFromString<PlacesResponse>(body).places.single()
        assertEquals("https://example.test/api/v1/place-icons/forest.png", place.imageUrl)
    }
}
