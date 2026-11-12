package com.dozy.mobile

import com.dozy.mobile.feature.place.data.database.PlaceDao
import com.dozy.mobile.feature.place.data.database.PlaceEntity
import kotlinx.coroutines.flow.MutableStateFlow

internal class FakePlaceDao : PlaceDao() {
    val rows = MutableStateFlow<List<PlaceEntity>>(emptyList())
    var failure = false

    override fun observe() = rows

    override suspend fun find(id: String) = rows.value.find { it.id == id }

    override suspend fun upsert(place: PlaceEntity) {
        check(!failure)
        rows.value = rows.value.filterNot { it.id == place.id } + place
    }

    override suspend fun pending() = rows.value.filter { it.local && it.pendingSync }

    override suspend fun deleteRemote() {
        rows.value = rows.value.filter { it.local }
    }
}
