package com.dozy.mobile.feature.place.data

import com.dozy.mobile.feature.place.data.api.PlaceApi
import com.dozy.mobile.feature.place.data.database.PlaceDao
import com.dozy.mobile.feature.place.data.database.PlaceEntity
import com.dozy.mobile.feature.place.domain.model.Place
import com.dozy.mobile.feature.place.domain.model.PlaceSync
import com.dozy.mobile.feature.place.domain.repository.PlaceRepository
import com.kjursa.foundation.coroutines.runCatchingSuspend
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class DefaultPlaceRepository(
    private val dao: PlaceDao,
    private val api: PlaceApi,
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
            val response = api.list() ?: return@withLock false
            val rows = response.places.map { it.toEntity() }
            require(rows.map { it.id }.distinct().size == rows.size)
            dao.replaceRemote(rows)
            true
        }

    override suspend fun save(place: Place): PlaceSync =
        mutations.withLock {
            require(place.canEdit)
            val row = place.toEntity()
            dao.upsert(row)
            synchronize(row)
        }

    private suspend fun synchronize(row: PlaceEntity): PlaceSync {
        val acknowledged = runCatchingSuspend { api.save(row.toDomain().toDto()) }.getOrDefault(false)
        if (!acknowledged) return PlaceSync.PENDING
        val updated = runCatchingSuspend { dao.upsert(row.copy(pendingSync = false)) }
        return if (updated.isSuccess) PlaceSync.SYNCED else PlaceSync.PENDING
    }
}
