package com.dozy.mobile.feature.story.data.database

import androidx.room3.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "story_generations")
data class GenerationEntity(
    @PrimaryKey val requestKey: String,
    val configuration: String,
    val jobId: String? = null,
    val status: String? = null,
)

@Dao
interface GenerationDao {
    @Query("SELECT * FROM story_generations WHERE requestKey = :key")
    suspend fun find(key: String): GenerationEntity?

    @Query("SELECT * FROM story_generations")
    fun observe(): Flow<List<GenerationEntity>>

    @Upsert
    suspend fun upsert(entity: GenerationEntity)
}
