package com.dozy.features.place.domain.repository

import com.dozy.domain.VersionedCollection
import com.dozy.domain.id.IdempotencyKey
import com.dozy.domain.id.OwnerId
import com.dozy.domain.id.PlaceId
import com.dozy.features.place.domain.model.Place
import com.dozy.features.place.domain.model.SavePlace
import com.dozy.features.place.domain.model.SavePlaceResult

interface PlaceRepository {
    suspend fun list(ownerId: OwnerId): List<Place> = checkNotNull(list(ownerId) { false }.items)

    suspend fun list(
        ownerId: OwnerId,
        isCurrent: (String) -> Boolean,
    ): VersionedCollection<Place>
    suspend fun create(
        ownerId: OwnerId,
        command: SavePlace,
        requestKey: IdempotencyKey? = null
    ): SavePlaceResult
    suspend fun save(
        ownerId: OwnerId,
        id: PlaceId,
        command: SavePlace
    ): SavePlaceResult
}
