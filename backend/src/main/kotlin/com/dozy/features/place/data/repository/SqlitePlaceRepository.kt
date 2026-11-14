package com.dozy.features.place.data.repository

import com.dozy.features.installation.data.database.registerInstallation
import com.dozy.features.installation.domain.model.InstallationId
import com.dozy.features.place.data.database.existingPlace
import com.dozy.features.place.data.database.upsertPlace
import com.dozy.features.place.data.database.visiblePlaces
import com.dozy.features.place.domain.model.Place
import com.dozy.features.place.domain.model.SavePlace
import com.dozy.features.place.domain.model.SavePlaceResult
import com.dozy.features.place.domain.repository.PlaceRepository
import com.dozy.infrastructure.database.SqliteDatabase

class SqlitePlaceRepository(private val database: SqliteDatabase) : PlaceRepository {
    override suspend fun list(installation: InstallationId): List<Place> = database.write {
        registerInstallation(installation)
        visiblePlaces(installation)
    }

    override suspend fun save(installation: InstallationId, id: Place.Id, command: SavePlace): SavePlaceResult =
        database.write {
            val existing = existingPlace(id)
            when {
                id.value.startsWith("builtin:") -> SavePlaceResult.Forbidden
                existing != null && existing.installationId != installation.value -> SavePlaceResult.Conflict
                else -> {
                    registerInstallation(installation)
                    upsertPlace(installation, id, command)
                    if (existing == null) SavePlaceResult.Created else SavePlaceResult.Updated
                }
            }
        }
}
