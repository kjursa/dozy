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
    @ColumnInfo(defaultValue = "NULL") val errorCode: String? = null,
    @ColumnInfo(defaultValue = "0") val statusUnknown: Boolean = false,
    @ColumnInfo(defaultValue = "NULL") val retryKey: String? = null,
    @ColumnInfo(defaultValue = "0") val createdOrder: Long = 0,
)

data class GenerationProgress(
    val jobId: String?,
    val status: String?,
    val storyId: String?,
    val errorCode: String?,
    val statusUnknown: Boolean,
    val notificationDelivered: Boolean,
    val retryKey: String?,
    val createdOrder: Long,
)

@Dao
interface GenerationDao {
    @Query("SELECT * FROM story_generations WHERE requestKey = :key")
    suspend fun find(key: String): GenerationEntity?

    @Query("SELECT * FROM story_generations WHERE jobId = :jobId")
    suspend fun findJob(jobId: String): GenerationEntity?

    @Query("SELECT * FROM story_generations ORDER BY createdOrder")
    fun observe(): Flow<List<GenerationEntity>>

    @Query(
        """
        SELECT jobId, status, NULL AS storyId, errorCode, statusUnknown,
               0 AS notificationDelivered, retryKey, createdOrder
        FROM story_generations
        UNION ALL
        SELECT jobId, 'READY', id, NULL, 0, notificationDelivered, NULL, createdOrder
        FROM stories WHERE requestKey IS NOT NULL AND result IS NOT NULL
        ORDER BY createdOrder
        """,
    )
    fun observeProgress(): Flow<List<GenerationProgress>>

    @Query("SELECT * FROM stories ORDER BY id DESC")
    fun observeStories(): Flow<List<StoryEntity>>

    @Query("SELECT * FROM stories WHERE id = :id")
    suspend fun findStory(id: String): StoryEntity?

    @Query("SELECT * FROM stories WHERE requestKey = :key AND result IS NOT NULL")
    suspend fun findStoryForRequest(key: String): StoryEntity?

    @Query("SELECT * FROM stories WHERE jobId = :jobId AND requestKey IS NOT NULL AND result IS NOT NULL")
    suspend fun findStoryForJob(jobId: String): StoryEntity?

    @Query("DELETE FROM stories WHERE result IS NULL")
    suspend fun clearLibrary()

    @Query("UPDATE stories SET summary = :summary WHERE id = :id AND result IS NOT NULL")
    suspend fun updateSummary(
        id: String,
        summary: String?,
    ): Int

    @Transaction
    suspend fun replaceLibrary(entities: List<StoryEntity>) {
        clearLibrary()
        entities.forEach {
            if (updateSummary(it.id, it.summary) == 0) upsertStory(it)
        }
    }

    @Query("DELETE FROM story_generations WHERE requestKey = :key")
    suspend fun deleteGeneration(key: String)

    @Transaction
    suspend fun completeGeneration(
        key: String,
        story: StoryEntity,
    ) {
        upsertStory(story)
        deleteGeneration(key)
    }

    @Query("UPDATE stories SET notificationDelivered = 1 WHERE jobId = :jobId AND requestKey IS NOT NULL")
    suspend fun markNotificationDelivered(jobId: String)

    @Upsert
    suspend fun upsertStory(story: StoryEntity)

    @Upsert
    suspend fun upsert(entity: GenerationEntity)
}
