package com.dozy.mobile.data.character.database

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface CharacterDao {
    @Query("SELECT * FROM characters ORDER BY name COLLATE NOCASE, id")
    fun observe(): Flow<List<CharacterEntity>>

    @Upsert
    suspend fun upsert(character: CharacterEntity)
}
