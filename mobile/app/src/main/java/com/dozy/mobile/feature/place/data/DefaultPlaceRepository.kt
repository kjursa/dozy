package com.dozy.mobile.feature.place.data

import com.dozy.mobile.feature.place.data.api.PlaceApi
import com.dozy.mobile.feature.place.data.database.PlaceDao
import com.dozy.mobile.feature.place.domain.model.Place
import com.dozy.mobile.feature.place.domain.repository.PlaceRepository
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

class DefaultPlaceRepository(
    private val dao: PlaceDao,
    private val api: PlaceApi,
    private val bundled: List<Place>,
) : PlaceRepository {
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

    override suspend fun refresh(): Boolean {
        val response = api.list() ?: return false
        val rows = response.places.map { it.toEntity() }
        require(rows.map { it.id }.distinct().size == rows.size)
        dao.replaceRemote(rows)
        return true
    }

    override suspend fun save(place: Place) {
        require(place.canEdit)
        dao.upsert(place.toEntity())
    }
}
