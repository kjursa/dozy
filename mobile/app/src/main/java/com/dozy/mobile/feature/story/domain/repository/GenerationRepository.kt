package com.dozy.mobile.feature.story.domain.repository

import com.dozy.mobile.feature.story.domain.model.GenerationConfiguration
import com.dozy.mobile.feature.story.domain.model.GenerationJob
import com.dozy.mobile.feature.story.domain.model.Story
import com.dozy.mobile.feature.story.domain.model.StorySummary
import kotlinx.coroutines.flow.Flow

interface GenerationRepository {
    fun observe(): Flow<List<GenerationJob>>

    fun stories(): Flow<List<Story>>

    fun summaries(): Flow<List<StorySummary>>

    /** False means the backend is disabled. Failures throw without removing local data. */
    suspend fun refreshStories(): Boolean

    /** Local-first durable read. Null means a confirmed missing story; failures throw. */
    suspend fun readStory(id: Story.Id): Story?

    suspend fun refresh(id: GenerationJob.Id): GenerationJob?

    suspend fun retry(
        id: GenerationJob.Id,
        key: GenerationJob.RequestKey,
    ): GenerationJob?

    suspend fun notificationDelivered(id: GenerationJob.Id)

    suspend fun configuration(key: GenerationJob.RequestKey): GenerationConfiguration?

    suspend fun accepted(key: GenerationJob.RequestKey): GenerationJob?

    suspend fun start(
        key: GenerationJob.RequestKey,
        configuration: GenerationConfiguration,
    ): GenerationJob?
}
