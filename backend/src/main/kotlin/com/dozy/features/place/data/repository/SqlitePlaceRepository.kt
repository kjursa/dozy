package com.dozy.features.place.data.repository

import com.dozy.features.installation.data.database.registerInstallation
import com.dozy.features.installation.domain.model.InstallationId
import com.dozy.features.place.data.database.existingPlace
import com.dozy.features.place.data.database.insertPlace
import com.dozy.features.place.data.database.updatePlace
import com.dozy.features.place.data.database.visiblePlaces
import com.dozy.features.place.domain.model.Place
import com.dozy.features.place.domain.model.SavePlace
import com.dozy.features.place.domain.model.SavePlaceResult
import com.dozy.features.place.domain.repository.PlaceRepository
import com.dozy.infrastructure.database.ResourceCreation
import com.dozy.infrastructure.database.SqliteDatabase
import com.dozy.infrastructure.id.Uuids

class SqlitePlaceRepository(private val database: SqliteDatabase) : PlaceRepository {
    override suspend fun list(installation: InstallationId): List<Place> = database.write {
        registerInstallation(installation)
        visiblePlaces(installation)
    }

    override suspend fun create(
        installation: InstallationId,
        command: SavePlace,
        requestKey: String?
    ): SavePlaceResult = database.write {
        val creation = requestKey?.let {
            ResourceCreation(
                installation,
                "place",
                it,
                listOf(command.name.value, command.description.value, command.inhabitants?.value)
            )
        }
        creation?.previous()?.let {
            if (!it.sameInput) return@write SavePlaceResult.IdempotencyConflict
            return@write SavePlaceResult.Created(Place.Id(it.id))
        }
        val id = Place.Id(Uuids.v7())
        registerInstallation(installation)
        insertPlace(installation, id, command)
        creation?.record(id.value)
        SavePlaceResult.Created(id)
    }

    override suspend fun save(
        installation: InstallationId,
        id: Place.Id,
        command: SavePlace
    ): SavePlaceResult = database.write {
        val existing = existingPlace(id)
        when {
            id.value.startsWith("builtin:") -> SavePlaceResult.Forbidden
            existing == null -> SavePlaceResult.NotFound
            existing.installationId != installation.value -> SavePlaceResult.Conflict
            else -> {
                updatePlace(id, command)
                SavePlaceResult.Updated
            }
        }
    }
}
