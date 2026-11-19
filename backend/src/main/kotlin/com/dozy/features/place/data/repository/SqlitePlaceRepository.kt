package com.dozy.features.place.data.repository

import com.dozy.domain.id.IdempotencyKey
import com.dozy.domain.id.OwnerId
import com.dozy.domain.id.PlaceId
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
import com.dozy.infrastructure.database.registerOwnerPartition

class SqlitePlaceRepository(private val database: SqliteDatabase) : PlaceRepository {
    override suspend fun list(ownerId: OwnerId): List<Place> = database.write {
        registerOwnerPartition(ownerId)
        visiblePlaces(ownerId)
    }

    override suspend fun create(
        ownerId: OwnerId,
        command: SavePlace,
        requestKey: IdempotencyKey?
    ): SavePlaceResult = database.write {
        val creation = requestKey?.let {
            ResourceCreation(
                ownerId,
                "place",
                it,
                listOf(command.name.value, command.description.value, command.inhabitants?.value)
            )
        }
        creation?.previous()?.let {
            if (!it.sameInput) return@write SavePlaceResult.IdempotencyConflict
            return@write SavePlaceResult.Created(requireNotNull(PlaceId.parse(it.id)))
        }
        val id = PlaceId.generate()
        registerOwnerPartition(ownerId)
        insertPlace(ownerId, id, command)
        creation?.record(id.value)
        SavePlaceResult.Created(id)
    }

    override suspend fun save(
        ownerId: OwnerId,
        id: PlaceId,
        command: SavePlace
    ): SavePlaceResult = database.write {
        val existing = existingPlace(id)
        when {
            id.isBuiltIn -> SavePlaceResult.Forbidden
            existing == null -> SavePlaceResult.NotFound
            existing.ownerId != ownerId -> SavePlaceResult.Conflict
            else -> {
                updatePlace(id, command)
                SavePlaceResult.Updated
            }
        }
    }
}
