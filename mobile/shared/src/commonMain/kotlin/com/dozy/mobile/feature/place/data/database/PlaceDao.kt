package com.dozy.mobile.feature.place.data.database

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
abstract class PlaceDao {
    @Query("SELECT * FROM places")
    abstract fun observe(): Flow<List<PlaceEntity>>

    @Query("SELECT * FROM places WHERE id = :id")
    abstract suspend fun find(id: String): PlaceEntity?

    @Query("SELECT * FROM places WHERE local = 1 AND pendingSync = 1")
    abstract suspend fun pending(): List<PlaceEntity>

    @Upsert
    abstract suspend fun upsert(place: PlaceEntity)

    @Query("DELETE FROM places WHERE local = 0")
    abstract suspend fun deleteRemote()

    @Transaction
    open suspend fun replaceRemote(places: List<PlaceEntity>) {
        deleteRemote()
        places.forEach { if (find(it.id)?.local != true) upsert(it) }
    }
}
