package com.dozy.mobile

import com.dozy.mobile.core.data.database.SyncState
import com.dozy.mobile.feature.place.data.database.PlaceDao
import com.dozy.mobile.feature.place.data.database.PlaceEntity
import fixtures.FakeSyncStateQueries
import kotlinx.coroutines.flow.MutableStateFlow

internal class FakePlaceDao : PlaceDao() {
    private val syncState = FakeSyncStateQueries()

    override suspend fun syncValue(collection: String) = syncState.syncValue(collection)

    override suspend fun saveSyncValue(state: SyncState) = syncState.saveSyncValue(state)

    val rows = MutableStateFlow<List<PlaceEntity>>(emptyList())
    var failure = false

    override fun observe() = rows

    override suspend fun find(id: String) = rows.value.find { it.id == id }

    override suspend fun upsert(place: PlaceEntity) {
        check(!failure)
        rows.value = rows.value.filterNot { it.id == place.id } + place
    }

    override suspend fun pending() = rows.value.filter { it.local && it.pendingSync }

    override suspend fun deleteSynced() {
        rows.value = rows.value.filter { it.pendingSync }
    }
}
