package com.dozy.api.client.place.v1

import com.dozy.api.contracts.v1.place.SavePlaceRequest

interface PlacesClient {
    suspend fun list(ifNoneMatch: String? = null): PlacesListResult
    suspend fun create(request: SavePlaceRequest, requestKey: String? = null): PlacesCreateResult

    suspend fun save(id: String, request: SavePlaceRequest): PlacesSaveResult
}
