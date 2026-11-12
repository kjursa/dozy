package com.dozy.mobile.feature.story.data.database

import androidx.room3.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "story_generations")
data class GenerationEntity(
    @PrimaryKey val requestKey: String,
    val configuration: String,
    val jobId: String? = null,
    val status: String? = null,
    @ColumnInfo(defaultValue = "NULL") val result: String? = null,
    @ColumnInfo(defaultValue = "NULL") val errorCode: String? = null,
    @ColumnInfo(defaultValue = "0") val statusUnknown: Boolean = false,
    @ColumnInfo(defaultValue = "NULL") val retryKey: String? = null,
    @ColumnInfo(defaultValue = "0") val notificationDelivered: Boolean = false,
    @ColumnInfo(defaultValue = "0") val createdOrder: Long = 0,
    @ColumnInfo(defaultValue = "NULL") val summary: String? = null,
)

@Dao
interface GenerationDao {
    @Query("SELECT * FROM story_generations WHERE requestKey = :key")
    suspend fun find(key: String): GenerationEntity?

    @Query("SELECT * FROM story_generations ORDER BY createdOrder")
    fun observe(): Flow<List<GenerationEntity>>

    @Upsert
    suspend fun upsert(entity: GenerationEntity)
}
