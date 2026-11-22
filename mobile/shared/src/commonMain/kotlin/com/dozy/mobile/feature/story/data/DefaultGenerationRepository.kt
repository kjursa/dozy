package com.dozy.mobile.feature.story.data

import com.dozy.api.client.ApiDisabledException
import com.dozy.api.client.TechnicalError
import com.dozy.api.client.story.StoriesClients
import com.dozy.api.client.story.v1.StoryReadResult
import com.dozy.api.client.story.v1.StorySyncResult
import com.dozy.api.client.storygeneration.StoryGenerationsClients
import com.dozy.api.client.storygeneration.v1.GenerationRetryResult
import com.dozy.api.client.storygeneration.v1.GenerationStartResult
import com.dozy.api.client.storygeneration.v1.GenerationStatusResult
import com.dozy.api.contracts.v1.story.GenerationRequestDto
import com.dozy.api.contracts.v1.story.GenerationRetryError
import com.dozy.api.contracts.v1.story.StoryDto
import com.dozy.api.contracts.v1.story.StoryReadError
import com.dozy.api.contracts.v1.story.StorySummaryDto
import com.dozy.mobile.feature.story.data.database.GenerationDao
import com.dozy.mobile.feature.story.data.database.GenerationEntity
import com.dozy.mobile.feature.story.data.database.STORIES_SYNC
import com.dozy.mobile.feature.story.data.database.StoryEntity
import com.dozy.mobile.feature.story.domain.model.GenerationConfiguration
import com.dozy.mobile.feature.story.domain.model.GenerationJob
import com.dozy.mobile.feature.story.domain.model.Story
import com.dozy.mobile.feature.story.domain.repository.GenerationRepository
import com.kjursa.foundation.coroutines.runCatchingSuspend
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json

/** Persist before sending so uncertain responses can safely replay the exact same snapshots. */
class DefaultGenerationRepository(
    private val dao: GenerationDao,
    private val generations: StoryGenerationsClients,
    private val reader: StoriesClients,
    private val allowLocalAssetHttp: Boolean = false,
) : GenerationRepository {
    private val mutex = Mutex()

    override fun observe() = dao.observeProgress().map { rows -> rows.mapNotNull { it.toJob() } }

    override fun stories() =
        dao.observeStories().map { rows ->
            rows.mapNotNull { it.result?.let { json -> Json.decodeFromString<StoryDto>(json).toDomain(allowLocalAssetHttp) } }
        }

    override fun summaries() =
        dao.observeStories().map { rows ->
            rows.mapNotNull { row ->
                val summary =
                    row.summary?.let { Json.decodeFromString<StorySummaryDto>(it) }
                        ?: row.result?.let { Json.decodeFromString<StoryDto>(it).toSummaryDto() }
                summary?.toDomain(allowDevelopmentHttp = true)
            }
        }

    override suspend fun refreshStories(): Boolean =
        mutex.withLock {
            val revision = dao.syncValue(STORIES_SYNC)?.toLong()
            val after = revision ?: 0L
            val changes =
                when (val response = reader.v1.sync(after)) {
                    is StorySyncResult.Success -> response.response
                    is StorySyncResult.ApiError -> error("Library unavailable")
                    is StorySyncResult.TechnicalError -> {
                        if ((response.error as? TechnicalError.Communication)?.exception is ApiDisabledException) return@withLock false
                        error("Library unavailable")
                    }
                }
            val upserts =
                changes.upserts.map { summary ->
                    summary.toDomain(allowDevelopmentHttp = true)
                    StoryEntity(summary.id, summary = Json.encodeToString(summary))
                }
            if (revision == changes.nextRevision) return@withLock true
            dao.applyStoryChanges(upserts, changes.deletedIds, changes.nextRevision, initialSync = revision == null)
            true
        }

    override suspend fun readStory(id: Story.Id): Story? =
        mutex.withLock {
            dao.findStory(id.value)?.result?.let { return@withLock Json.decodeFromString<StoryDto>(it).toDomain(allowLocalAssetHttp) }
            val dto =
                when (val response = reader.v1.read(id.value)) {
                    is StoryReadResult.Success -> response.response
                    is StoryReadResult.TechnicalError -> error("Story unavailable")
                    is StoryReadResult.ApiError ->
                        if (response.error ==
                            StoryReadError.NotFound
                        ) {
                            return@withLock null
                        } else {
                            error("Story unavailable")
                        }
                }
            require(dto.id == id.value)
            val story = dto.toDomain(allowLocalAssetHttp)
            saveStory(dto, dto.jobId?.let { dao.findJob(it) })
            story
        }

    private suspend fun saveStory(
        dto: StoryDto,
        generation: GenerationEntity?,
    ): StoryEntity {
        val entity =
            StoryEntity(
                id = dto.id,
                jobId = generation?.jobId ?: dto.jobId,
                requestKey = generation?.requestKey,
                result = Json.encodeToString(dto),
                summary = dao.findStory(dto.id)?.summary,
                createdOrder = generation?.createdOrder ?: 0,
            )
        if (generation == null) {
            dao.upsertStory(entity)
        } else {
            dao.completeGeneration(generation.requestKey, entity)
        }
        return entity
    }

    override suspend fun refresh(id: GenerationJob.Id): GenerationJob? =
        mutex.withLock {
            val saved = dao.findJob(id.value) ?: return@withLock dao.findStoryForJob(id.value)?.toCompletedJob()
            if (saved.retryKey != null) return@withLock saved.toJob()
            runCatchingSuspend {
                val remote =
                    when (val response = generations.v1.status(id.value)) {
                        is GenerationStatusResult.Success -> response.response
                        is GenerationStatusResult.ApiError, is GenerationStatusResult.TechnicalError -> error("Status unavailable")
                    }
                require(remote.jobId == id.value)
                if (remote.status == "READY") {
                    val story = requireNotNull(remote.story)
                    require(story.id == remote.storyId && story.jobId == id.value)
                    story.toDomain(allowLocalAssetHttp)
                    return@runCatchingSuspend saveStory(story, saved).toCompletedJob()
                }
                val updated =
                    saved.copy(
                        status = remote.status,
                        errorCode = remote.errorCode,
                        statusUnknown = false,
                        retryKey = if (remote.status == "FAILED") saved.retryKey else null,
                    )
                updated.toJob().also { dao.upsert(updated) }
            }.getOrElse {
                saved.copy(statusUnknown = true).also { dao.upsert(it) }.toJob()
            }
        }

    override suspend fun retry(
        id: GenerationJob.Id,
        key: GenerationJob.RequestKey,
    ): GenerationJob? =
        mutex.withLock {
            val saved = dao.findJob(id.value) ?: return@withLock null
            if (saved.status != "FAILED" && saved.retryKey == null) return@withLock saved.toJob()
            check(dao.observe().first().none { it.jobId != id.value && (it.status in activeStatuses || it.retryKey != null) })
            if (saved.statusUnknown && saved.retryKey == null) return@withLock null
            val pending = saved.copy(retryKey = saved.retryKey ?: key.value)
            dao.upsert(pending)
            val response =
                when (val result = generations.v1.retry(id.value, requireNotNull(pending.retryKey))) {
                    is GenerationRetryResult.Success -> result.response
                    is GenerationRetryResult.TechnicalError -> return@withLock null
                    is GenerationRetryResult.ApiError -> {
                        if (result.error !is GenerationRetryError.Unknown) {
                            dao.upsert(pending.copy(retryKey = null, statusUnknown = true))
                        }
                        return@withLock null
                    }
                }
            require(response.jobId == id.value)
            val updated =
                pending.copy(
                    status = response.status,
                    errorCode =
                        if (response.status ==
                            "FAILED"
                        ) {
                            saved.errorCode
                        } else {
                            null
                        },
                    statusUnknown = response.status in setOf("READY", "FAILED"),
                    retryKey = null,
                )
            dao.upsert(updated)
            updated.toJob()
        }

    override suspend fun notificationDelivered(id: GenerationJob.Id) =
        mutex.withLock {
            dao.markNotificationDelivered(id.value)
        }

    private val activeStatuses = setOf("QUEUED", "CREATING_TEXT", "CREATING_COVER")

    override suspend fun configuration(key: GenerationJob.RequestKey): GenerationConfiguration? =
        dao.find(key.value)?.let { Json.decodeFromString<GenerationRequestDto>(it.configuration).toDomain(allowLocalAssetHttp) }

    override suspend fun accepted(key: GenerationJob.RequestKey) =
        dao.find(key.value)?.toJob() ?: dao.findStoryForRequest(key.value)?.toCompletedJob()

    override suspend fun start(
        key: GenerationJob.RequestKey,
        configuration: GenerationConfiguration,
    ): GenerationJob? =
        mutex.withLock {
            dao.findStoryForRequest(key.value)?.toCompletedJob()?.let { return@withLock it }
            val row =
                dao.find(key.value)
                    ?: GenerationEntity(
                        key.value,
                        Json.encodeToString(configuration.toDto()),
                        createdOrder =
                            (dao.observeProgress().first().maxOfOrNull { it.createdOrder } ?: 0) + 1,
                    ).also { dao.upsert(it) }
            row.toJob()?.let { return@withLock it }
            check(dao.observe().first().none { it.requestKey != key.value && (it.status in activeStatuses || it.retryKey != null) })
            val response =
                when (val response = generations.v1.start(key.value, Json.decodeFromString<GenerationRequestDto>(row.configuration))) {
                    is GenerationStartResult.Success -> response.response
                    is GenerationStartResult.ApiError -> return@withLock null
                    is GenerationStartResult.TechnicalError -> {
                        if ((response.error as? TechnicalError.Communication)?.exception is ApiDisabledException) return@withLock null
                        error("Story submission unavailable")
                    }
                }
            val accepted = row.copy(jobId = response.jobId, status = response.status)
            dao.upsert(accepted)
            accepted.toJob()
        }
}
