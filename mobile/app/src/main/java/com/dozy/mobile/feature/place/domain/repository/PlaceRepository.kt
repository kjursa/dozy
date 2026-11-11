package com.dozy.mobile.feature.place.domain.repository

import com.dozy.mobile.feature.place.domain.model.Place
import com.dozy.mobile.feature.place.domain.model.PlaceSync
import kotlinx.coroutines.flow.Flow

interface PlaceRepository {
    fun observe(): Flow<List<Place>>

    suspend fun refresh(): Boolean

    suspend fun find(id: Place.Id): Place?

    suspend fun save(place: Place): PlaceSync
}
