package com.dozy.api.client.place.v1

import com.dozy.api.client.ApiResult
import com.dozy.api.contracts.v1.place.PlacesResponse
import com.dozy.api.contracts.v1.place.SavePlaceRequest

interface PlacesClient {
    suspend fun list(): ApiResult<PlacesResponse, PlacesListError>

    suspend fun save(id: String, request: SavePlaceRequest): ApiResult<Unit, PlacesSaveError>
}
