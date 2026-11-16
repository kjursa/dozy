package com.dozy.mobile.feature.story.data

import com.dozy.api.client.ApiDisabledException
import com.dozy.api.client.TechnicalError
import com.dozy.api.client.story.StoriesClients
import com.dozy.api.client.story.v1.StoryListResult
import com.dozy.api.client.story.v1.StoryReadResult
import com.dozy.api.client.storygeneration.StoryGenerationsClients
import com.dozy.api.client.storygeneration.v1.GenerationRetryResult
import com.dozy.api.client.storygeneration.v1.GenerationStartResult
import com.dozy.api.client.storygeneration.v1.GenerationStatusResult
import com.dozy.api.contracts.v1.story.GenerationRequestDto
import com.dozy.api.contracts.v1.story.GenerationRetryError
import com.dozy.api.contracts.v1.story.ListStoriesRequest
import com.dozy.api.contracts.v1.story.StoryDto
import com.dozy.api.contracts.v1.story.StoryReadError
import com.dozy.api.contracts.v1.story.StorySummaryDto
import com.dozy.mobile.feature.story.data.database.GenerationDao
import com.dozy.mobile.feature.story.data.database.GenerationEntity
import com.dozy.mobile.feature.story.domain.model.GenerationConfiguration
import com.dozy.mobile.feature.story.domain.model.GenerationJob
import com.dozy.mobile.feature.story.domain.model.Story
import com.dozy.mobile.feature.story.domain.model.StorySummary
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
) : GenerationRepository {
    private val mutex = Mutex()

    override fun observe() = dao.observe().map { rows -> rows.mapNotNull { it.toJob() } }

    override fun stories() =
        dao.observe().map { rows ->
            rows.mapNotNull { it.result?.let { json -> Json.decodeFromString<StoryDto>(json).toDomain() } }.distinctBy { it.id }
        }

    override fun summaries() =
        dao.observe().map { rows ->
            rows
                .mapNotNull { row ->
                    row.summary?.let { Json.decodeFromString<StorySummaryDto>(it).toDomain(allowDevelopmentHttp = true) }
                        ?: row.result?.let { Json.decodeFromString<StoryDto>(it).toSummaryDto().toDomain(allowDevelopmentHttp = true) }
                }.groupBy { it.id }
                .values
                .map { versions -> versions.maxBy { it.createdAt.epochMillis } }
                .sortedWith(compareByDescending<StorySummary> { it.createdAt.epochMillis }.thenBy { it.id.value })
        }

    override suspend fun refreshStories(): Boolean =
        mutex.withLock {
            val collected = mutableListOf<StorySummaryDto>()
            val cursors = mutableSetOf<String>()
            var cursor: String? = null
            do {
                val response = reader.v1.list(ListStoriesRequest(cursor = cursor))
                val page =
                    when (response) {
                        is StoryListResult.Success -> response.response
                        is StoryListResult.ApiError -> error("Library unavailable")
                        is StoryListResult.TechnicalError -> {
                            if ((response.error as? TechnicalError.Communication)?.exception is ApiDisabledException) return@withLock false
                            error("Library unavailable")
                        }
                    }
                require(page.stories.isNotEmpty() || page.nextCursor == null)
                page.stories.forEach { it.toDomain(allowDevelopmentHttp = true) }
                collected += page.stories
                cursor = page.nextCursor
                if (cursor != null) require(cursor.isNotBlank() && cursors.add(cursor))
            } while (cursor != null)
            require(collected.distinctBy { it.id.lowercase() }.size == collected.size)
            dao.replaceLibrary(
                collected.map { summary ->
                    GenerationEntity(requestKey = "library:${summary.id}", configuration = "", summary = Json.encodeToString(summary))
                },
            )
            true
        }

    override suspend fun readStory(id: Story.Id): Story? =
        mutex.withLock {
            stories().first().find { it.id == id }?.let { return@withLock it }
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
            val story = dto.toDomain()
            dao.upsert(
                GenerationEntity(
                    requestKey = "story:${id.value}",
                    configuration = Json.encodeToString(dto.configuration),
                    result = Json.encodeToString(dto),
                ),
            )
            story
        }

    private suspend fun row(id: GenerationJob.Id) = dao.observe().first().find { it.jobId == id.value }

    override suspend fun refresh(id: GenerationJob.Id): GenerationJob? =
        mutex.withLock {
            val saved = row(id) ?: return@withLock null
            if (saved.result != null || saved.retryKey != null) return@withLock saved.toJob()
            runCatchingSuspend {
                val remote =
                    when (val response = generations.v1.status(id.value)) {
                        is GenerationStatusResult.Success -> response.response
                        is GenerationStatusResult.ApiError, is GenerationStatusResult.TechnicalError -> error("Status unavailable")
                    }
                require(remote.jobId == id.value)
                val result =
                    if (remote.status == "READY") {
                        val story =
                            when (val response = reader.v1.read(requireNotNull(remote.storyId))) {
                                is StoryReadResult.Success -> response.response
                                is StoryReadResult.ApiError, is StoryReadResult.TechnicalError -> error("Story unavailable")
                            }
                        require(story.id == remote.storyId)
                        // Validate snapshots before committing READY. One row stores status and result atomically.
                        story.toDomain()
                        Json.encodeToString(story)
                    } else {
                        null
                    }
                val updated =
                    saved.copy(
                        status = remote.status,
                        errorCode = remote.errorCode,
                        statusUnknown = false,
                        result = result,
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
            val saved = row(id) ?: return@withLock null
            if ((saved.status != "FAILED" && saved.retryKey == null) || saved.result != null) return@withLock saved.toJob()
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
            row(id)?.let { dao.upsert(it.copy(notificationDelivered = true)) }
            Unit
        }

    private val activeStatuses = setOf("QUEUED", "CREATING_TEXT", "CREATING_COVER")

    override suspend fun configuration(key: GenerationJob.RequestKey): GenerationConfiguration? =
        dao.find(key.value)?.let { Json.decodeFromString<GenerationRequestDto>(it.configuration).toDomain() }

    override suspend fun accepted(key: GenerationJob.RequestKey) = dao.find(key.value)?.toJob()

    override suspend fun start(
        key: GenerationJob.RequestKey,
        configuration: GenerationConfiguration,
    ): GenerationJob? =
        mutex.withLock {
            val row =
                dao.find(key.value)
                    ?: GenerationEntity(
                        key.value,
                        Json.encodeToString(configuration.toDto()),
                        createdOrder =
                            (dao.observe().first().maxOfOrNull { it.createdOrder } ?: 0) + 1,
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
