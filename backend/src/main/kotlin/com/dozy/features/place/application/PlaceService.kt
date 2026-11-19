package com.dozy.features.place.application

import com.dozy.domain.id.IdempotencyKey
import com.dozy.domain.id.OwnerId
import com.dozy.domain.id.PlaceId
import com.dozy.features.place.domain.model.Place
import com.dozy.features.place.domain.model.SavePlace
import com.dozy.features.place.domain.model.SavePlaceResult
import com.dozy.features.place.domain.repository.PlaceRepository

class PlaceService(private val repository: PlaceRepository) {
    suspend fun list(installation: OwnerId) = repository.list(installation).sortedWith(
        compareBy<Place> { it.origin != Place.Origin.USER }
            .thenBy { it.name.value.lowercase() }.thenBy { it.id.value },
    )

    suspend fun create(
        installation: OwnerId,
        command: SavePlace,
        requestKey: IdempotencyKey? = null
    ) = repository.create(installation, command, requestKey)

    suspend fun save(installation: OwnerId, id: PlaceId, command: SavePlace): SavePlaceResult {
        if (id.isBuiltIn) return SavePlaceResult.Forbidden
        return repository.save(installation, id, command)
    }
}
