package com.dozy.features.place.domain.repository

import com.dozy.features.installation.domain.model.InstallationId
import com.dozy.features.place.domain.model.Place
import com.dozy.features.place.domain.model.SavePlace
import com.dozy.features.place.domain.model.SavePlaceResult

interface PlaceRepository {
    suspend fun list(installation: InstallationId): List<Place>
    suspend fun save(installation: InstallationId, id: Place.Id, command: SavePlace): SavePlaceResult
}
