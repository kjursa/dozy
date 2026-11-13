package com.dozy.features.place.data.repository

import com.dozy.features.installation.data.database.registerInstallation
import com.dozy.features.installation.domain.model.InstallationId
import com.dozy.features.place.data.database.existingPlace
import com.dozy.features.place.data.database.upsertPlace
import com.dozy.features.place.data.database.visiblePlaces
import com.dozy.features.place.domain.model.Place
import com.dozy.features.place.domain.model.PlaceInput
import com.dozy.features.place.domain.model.SavePlaceResult
import com.dozy.features.place.domain.repository.PlaceRepository
import com.dozy.infrastructure.database.SqliteDatabase

class SqlitePlaceRepository(private val database: SqliteDatabase) : PlaceRepository {
    override suspend fun list(installation: InstallationId): List<Place> = database.write { connection ->
        connection.registerInstallation(installation)
        connection.visiblePlaces(installation)
    }

    override suspend fun save(installation: InstallationId, id: Place.Id, input: PlaceInput): SavePlaceResult =
        database.write { connection ->
            val existing = connection.existingPlace(id)
            when {
                id.value.startsWith("builtin:") -> SavePlaceResult.Forbidden
                existing != null && existing.installationId != installation.value -> SavePlaceResult.Conflict
                else -> {
                    connection.registerInstallation(installation)
                    connection.upsertPlace(installation, id, input)
                    if (existing == null) SavePlaceResult.Created else SavePlaceResult.Updated
                }
            }
        }
}
