package com.dozy.mobile.feature.story.data

import com.dozy.mobile.feature.story.data.api.*
import com.dozy.mobile.feature.story.data.database.*
import com.dozy.mobile.feature.story.domain.model.*
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
    private val api: GenerationApi,
) : GenerationRepository {
    private val mutex = Mutex()

    override fun observe() = dao.observe().map { rows -> rows.mapNotNull { it.toJob() } }

    override fun stories() =
        dao.observe().map { rows ->
            rows.mapNotNull { it.result?.let { json -> Json.decodeFromString<StoryDto>(json).toDomain() } }.distinctBy { it.id }
        }

    override suspend fun readStory(id: Story.Id): Story? =
        mutex.withLock {
            stories().first().find { it.id == id }?.let { return@withLock it }
            val dto =
                try {
                    api.story(id.value) ?: error("Story service unavailable")
                } catch (_: StoryNotFoundException) {
                    return@withLock null
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
            if (saved.result != null) return@withLock saved.toJob()
            runCatchingSuspend {
                val remote = api.status(id.value) ?: error("Status unavailable")
                require(remote.jobId == id.value)
                val result =
                    if (remote.status == "READY") {
                        val story = api.story(requireNotNull(remote.storyId)) ?: error("Story unavailable")
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
            if (saved.status != "FAILED" || saved.result != null) return@withLock saved.toJob()
            check(dao.observe().first().none { it.jobId != id.value && (it.status in activeStatuses || it.retryKey != null) })
            val pending = saved.copy(retryKey = saved.retryKey ?: key.value)
            dao.upsert(pending)
            val response = api.retry(id.value, requireNotNull(pending.retryKey)) ?: return@withLock null
            require(response.jobId == id.value)
            val updated = pending.copy(status = response.status, errorCode = null, statusUnknown = false, retryKey = null)
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
            val response = api.start(key.value, Json.decodeFromString<GenerationRequestDto>(row.configuration)) ?: return@withLock null
            val accepted = row.copy(jobId = response.jobId, status = response.status)
            dao.upsert(accepted)
            accepted.toJob()
        }
}
