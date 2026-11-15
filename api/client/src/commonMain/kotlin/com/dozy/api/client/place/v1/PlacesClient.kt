package com.dozy.api.client.place.v1

import com.dozy.api.contracts.v1.place.SavePlaceRequest

interface PlacesClient {
    suspend fun list(): PlacesListResult
    suspend fun save(id: String, request: SavePlaceRequest): PlacesSaveResult
}
