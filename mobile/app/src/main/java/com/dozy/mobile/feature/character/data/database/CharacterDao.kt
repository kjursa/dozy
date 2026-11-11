package com.dozy.mobile.feature.character.data.database

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface CharacterDao {
    @Query("SELECT * FROM characters ORDER BY name COLLATE NOCASE, id")
    fun observe(): Flow<List<CharacterEntity>>

    @Query("SELECT id FROM characters WHERE pendingSync = 1")
    suspend fun pendingIds(): List<String>

    @Query("DELETE FROM characters WHERE pendingSync = 0")
    suspend fun deleteSynced()

    /** One emission for a complete snapshot; local pending edits always win. */
    @Transaction
    suspend fun replaceSnapshot(characters: List<CharacterEntity>) {
        val pending = pendingIds().toSet()
        deleteSynced()
        characters.filterNot { it.id in pending }.forEach { upsert(it) }
    }

    @Upsert
    suspend fun upsert(character: CharacterEntity)
}
