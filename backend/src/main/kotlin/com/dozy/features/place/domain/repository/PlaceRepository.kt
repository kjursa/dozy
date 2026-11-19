package com.dozy.features.place.domain.repository

import com.dozy.domain.id.IdempotencyKey
import com.dozy.domain.id.OwnerId
import com.dozy.domain.id.PlaceId
import com.dozy.features.place.domain.model.Place
import com.dozy.features.place.domain.model.SavePlace
import com.dozy.features.place.domain.model.SavePlaceResult

interface PlaceRepository {
    suspend fun list(installation: OwnerId): List<Place>
    suspend fun create(
        installation: OwnerId,
        command: SavePlace,
        requestKey: IdempotencyKey? = null
    ): SavePlaceResult
    suspend fun save(
        installation: OwnerId,
        id: PlaceId,
        command: SavePlace
    ): SavePlaceResult
}
