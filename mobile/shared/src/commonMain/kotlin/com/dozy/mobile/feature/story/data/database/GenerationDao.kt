package com.dozy.mobile.feature.story.data.database

import androidx.room3.ColumnInfo
import androidx.room3.Dao
import androidx.room3.Entity
import androidx.room3.PrimaryKey
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Upsert
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

    @Query("DELETE FROM story_generations WHERE requestKey LIKE 'library:%'")
    suspend fun clearLibrary()

    @Upsert
    suspend fun upsertAll(entities: List<GenerationEntity>)

    @Transaction
    suspend fun replaceLibrary(entities: List<GenerationEntity>) {
        clearLibrary()
        upsertAll(entities)
    }

    @Upsert
    suspend fun upsert(entity: GenerationEntity)
}
