package com.dozy.mobile.feature.character.data.database

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface CharacterDao {
    @Query("SELECT * FROM characters WHERE deleted = 0 ORDER BY name COLLATE NOCASE, id")
    fun observe(): Flow<List<CharacterEntity>>

    @Query("SELECT * FROM characters WHERE id = :id AND deleted = 0")
    fun observe(id: String): Flow<CharacterEntity?>

    @Query("UPDATE characters SET deleted = 1, pendingSync = 1 WHERE id = :id")
    suspend fun markDeleted(id: String)

    @Query("SELECT id FROM characters WHERE deleted = 1")
    suspend fun deletedIds(): List<String>

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
