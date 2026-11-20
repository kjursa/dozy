package com.dozy.mobile.feature.place.data

import com.dozy.api.client.place.PlacesClients
import com.dozy.api.client.place.v1.PlacesCreateResult
import com.dozy.api.client.place.v1.PlacesListResult
import com.dozy.api.client.place.v1.PlacesSaveResult
import com.dozy.mobile.feature.place.data.database.PlaceDao
import com.dozy.mobile.feature.place.data.database.PlaceEntity
import com.dozy.mobile.feature.place.domain.model.Place
import com.dozy.mobile.feature.place.domain.model.PlaceInput
import com.dozy.mobile.feature.place.domain.model.PlaceSync
import com.dozy.mobile.feature.place.domain.repository.PlaceRepository
import com.kjursa.foundation.coroutines.runCatchingSuspend
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class DefaultPlaceRepository(
    private val dao: PlaceDao,
    private val places: PlacesClients,
    private val bundled: List<Place>,
) : PlaceRepository {
    private val mutations = Mutex()

    override fun observe() =
        dao
            .observe()
            .map { rows ->
                val saved = rows.map { it.toDomain() }
                saved + bundled.filterNot { fallback -> saved.any { it.id == fallback.id } }
            }.catch {
                emit(bundled)
                throw it
            }

    override suspend fun find(id: Place.Id) = dao.find(id.value)?.toDomain() ?: bundled.find { it.id == id }

    override suspend fun refresh(): Boolean =
        mutations.withLock {
            dao.pending().forEach { synchronize(it) }
            val response = places.v1.list()
            val snapshot =
                when (response) {
                    is PlacesListResult.Success -> response.response.places
                    is PlacesListResult.ApiError, is PlacesListResult.TechnicalError -> return@withLock false
                }
            val rows = snapshot.map { it.toEntity() }
            require(rows.map { it.id }.distinct().size == rows.size)
            dao.replaceRemote(rows)
            true
        }

    override suspend fun create(
        input: PlaceInput,
        requestKey: String?,
    ): Place =
        mutations.withLock {
            val result = places.v1.create(input.toSaveRequest(), requestKey)
            check(result is PlacesCreateResult.Success) { "Place creation failed" }
            val id = Place.Id(result.response.id)
            val place = Place(id, input.name, input.description, input.inhabitants, Place.Origin.USER, true, result.response.imageUrl)
            dao.upsert(place.toEntity().copy(pendingSync = false))
            place
        }

    override suspend fun save(place: Place): PlaceSync =
        mutations.withLock {
            require(place.canEdit)
            // Check the stored authority too: caller-supplied origin cannot grant edit permission.
            val existing = find(place.id)
            require(existing?.canEdit == true)
            val row = place.toEntity()
            dao.upsert(row)
            synchronize(row)
        }

    private suspend fun synchronize(row: PlaceEntity): PlaceSync {
        val acknowledged = places.v1.save(row.id, row.toDomain().toSaveRequest()) is PlacesSaveResult.Success
        if (!acknowledged) return PlaceSync.PENDING
        val updated = runCatchingSuspend { dao.upsert(row.copy(pendingSync = false)) }
        return if (updated.isSuccess) PlaceSync.SYNCED else PlaceSync.PENDING
    }
}
