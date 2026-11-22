package com.dozy.features.place.application

import com.dozy.domain.id.IdempotencyKey
import com.dozy.domain.id.OwnerId
import com.dozy.domain.id.PlaceId
import com.dozy.features.place.domain.model.Place
import com.dozy.features.place.domain.model.SavePlace
import com.dozy.features.place.domain.model.SavePlaceResult
import com.dozy.features.place.domain.repository.PlaceRepository

class PlaceService(
    private val repository: PlaceRepository,
) {
    suspend fun list(ownerId: OwnerId) = repository.list(ownerId).inDisplayOrder()

    suspend fun list(ownerId: OwnerId, isCurrent: (String) -> Boolean) =
        repository.list(ownerId, isCurrent).let { it.copy(items = it.items?.inDisplayOrder()) }

    private fun List<Place>.inDisplayOrder() = sortedWith(
        compareBy<Place> { it.origin != Place.Origin.USER }
            .thenBy { it.name.value.lowercase() }
            .thenBy { it.id.value },
    )

    suspend fun create(
        ownerId: OwnerId,
        command: SavePlace,
        requestKey: IdempotencyKey? = null,
    ) = repository.create(ownerId, command, requestKey)

    suspend fun save(
        ownerId: OwnerId,
        id: PlaceId,
        command: SavePlace,
    ): SavePlaceResult = repository.save(ownerId, id, command)
}
