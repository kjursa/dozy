package com.dozy.api.client.place

import com.dozy.api.contracts.v1.place.PlaceResponse
import com.dozy.api.contracts.v1.place.SavePlaceRequest

fun interface PlacesClient {
    suspend fun list(): List<PlaceResponse>

    suspend fun save(id: String, request: SavePlaceRequest) {
        throw UnsupportedOperationException("Place writes are not supported by this client")
    }
}
