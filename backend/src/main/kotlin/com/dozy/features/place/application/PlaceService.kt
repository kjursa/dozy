package com.dozy.features.place.application

import com.dozy.features.installation.domain.model.InstallationId
import com.dozy.features.place.domain.model.Place
import com.dozy.features.place.domain.model.PlaceInput
import com.dozy.features.place.domain.model.SavePlaceResult
import com.dozy.features.place.domain.repository.PlaceRepository

class PlaceService(private val repository: PlaceRepository) {
    suspend fun list(installation: InstallationId) = repository.list(installation).sortedWith(
        compareBy<Place> { it.ownership != Place.Ownership.CUSTOM }
            .thenBy { it.name.lowercase() }.thenBy { it.id.value },
    )

    suspend fun save(installation: InstallationId, id: Place.Id, input: PlaceInput): SavePlaceResult {
        if (id.value.startsWith("builtin:")) return SavePlaceResult.Forbidden
        return repository.save(installation, id, input)
    }
}
