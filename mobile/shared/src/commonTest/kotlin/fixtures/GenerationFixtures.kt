package com.dozy.mobile

import com.dozy.api.contracts.v1.story.GenerationAcceptedDto
import com.dozy.api.contracts.v1.story.GenerationRequestDto
import com.dozy.api.contracts.v1.story.GenerationStatusDto
import com.dozy.api.contracts.v1.story.StoryDto
import com.dozy.mobile.core.data.database.SyncStateQueries
import com.dozy.mobile.feature.story.data.api.GenerationApi
import com.dozy.mobile.feature.story.data.database.GenerationDao
import com.dozy.mobile.feature.story.data.database.GenerationEntity
import com.dozy.mobile.feature.story.data.database.GenerationProgress
import com.dozy.mobile.feature.story.data.database.StoryEntity
import com.dozy.mobile.feature.story.data.testGenerationRepository
import com.dozy.mobile.feature.story.domain.usecase.StartGeneration
import fixtures.FakeSyncStateQueries
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

internal class FakeGenerationDao :
    GenerationDao,
    SyncStateQueries by FakeSyncStateQueries() {
    private data class Tables(
        val generations: List<GenerationEntity> = emptyList(),
        val stories: List<StoryEntity> = emptyList(),
    )

    private val tables = MutableStateFlow(Tables())
    val rows get() = tables.value.generations

    override suspend fun find(key: String) = rows.find { it.requestKey == key }

    override suspend fun findJob(jobId: String) = rows.find { it.jobId == jobId }

    override fun observe() = tables.map { it.generations.sortedBy(GenerationEntity::createdOrder) }

    override fun observeProgress() =
        tables.map { table ->
            val pending =
                table.generations.map {
                    GenerationProgress(it.jobId, it.status, null, it.errorCode, it.statusUnknown, false, it.retryKey, it.createdOrder)
                }
            val ready =
                table.stories.filter { it.requestKey != null && it.result != null }.map {
                    GenerationProgress(it.jobId, "READY", it.id, null, false, it.notificationDelivered, null, it.createdOrder)
                }
            (pending + ready).sortedBy(GenerationProgress::createdOrder)
        }

    override fun observeStories() = tables.map { it.stories.sortedByDescending(StoryEntity::id) }

    override suspend fun findStory(id: String) = tables.value.stories.find { it.id == id }

    override suspend fun findStoryForRequest(key: String) = tables.value.stories.find { it.requestKey == key && it.result != null }

    override suspend fun findStoryForJob(jobId: String) =
        tables.value.stories.find { it.jobId == jobId && it.requestKey != null && it.result != null }

    override suspend fun storyIds() = tables.value.stories.map { it.id }

    override suspend fun deleteStories(ids: List<String>) {
        tables.update { it.copy(stories = it.stories.filterNot { story -> story.id in ids }) }
    }

    override suspend fun updateSummary(
        id: String,
        summary: String?,
    ): Int {
        val story = findStory(id)?.takeIf { it.result != null } ?: return 0
        upsertStory(story.copy(summary = summary))
        return 1
    }

    override suspend fun deleteGeneration(key: String) {
        tables.update { it.copy(generations = it.generations.filterNot { row -> row.requestKey == key }) }
    }

    override suspend fun completeGeneration(
        key: String,
        story: StoryEntity,
    ) {
        tables.update {
            it.copy(
                generations = it.generations.filterNot { row -> row.requestKey == key },
                stories = it.stories.filterNot { row -> row.id == story.id } + story,
            )
        }
    }

    override suspend fun markNotificationDelivered(jobId: String) {
        findStoryForJob(jobId)?.let { upsertStory(it.copy(notificationDelivered = true)) }
    }

    override suspend fun upsertStory(story: StoryEntity) {
        tables.update { it.copy(stories = it.stories.filterNot { row -> row.id == story.id } + story) }
    }

    override suspend fun upsert(entity: GenerationEntity) {
        tables.update { it.copy(generations = it.generations.filterNot { row -> row.requestKey == entity.requestKey } + entity) }
    }
}

internal fun testStartGeneration() =
    StartGeneration(
        testGenerationRepository(
            FakeGenerationDao(),
            object : GenerationApi {
                override suspend fun status(id: String): GenerationStatusDto? = null

                override suspend fun story(id: String): StoryDto? = null

                override suspend fun retry(
                    id: String,
                    key: String,
                ): GenerationAcceptedDto? = null

                override suspend fun start(
                    key: String,
                    request: GenerationRequestDto,
                ): GenerationAcceptedDto? = null
            },
        ),
        FakeCharacterRepository(),
        FakePlaceRepository(),
    )
