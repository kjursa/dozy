package com.dozy.mobile.feature.place.data.database

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Upsert
import com.dozy.mobile.core.data.database.SyncState
import com.dozy.mobile.core.data.database.SyncStateQueries
import kotlinx.coroutines.flow.Flow

@Dao
abstract class PlaceDao : SyncStateQueries {
    @Query("SELECT * FROM places")
    abstract fun observe(): Flow<List<PlaceEntity>>

    @Query("SELECT * FROM places WHERE id = :id")
    abstract suspend fun find(id: String): PlaceEntity?

    @Query("SELECT * FROM places WHERE local = 1 AND pendingSync = 1")
    abstract suspend fun pending(): List<PlaceEntity>

    @Upsert
    abstract suspend fun upsert(place: PlaceEntity)

    @Query("DELETE FROM places WHERE pendingSync = 0")
    abstract suspend fun deleteSynced()

    @Transaction
    open suspend fun replaceRemote(
        places: List<PlaceEntity>,
        etag: String? = null,
    ) {
        val pendingIds = pending().map { it.id }.toSet()
        deleteSynced()
        places.filterNot { it.id in pendingIds }.forEach { upsert(it) }
        saveSyncValue(SyncState(PLACES_SYNC, etag))
    }
}

internal const val PLACES_SYNC = "places"
