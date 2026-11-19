package com.dozy.features.story.data.cache

import com.dozy.domain.id.JobId
import com.dozy.domain.id.OwnerId
import com.dozy.domain.id.StoryId
import com.dozy.features.story.domain.model.Failure
import com.dozy.features.story.domain.model.Job
import com.dozy.features.story.domain.model.Stage
import com.dozy.features.story.domain.repository.JobStatusCache
import com.dozy.infrastructure.keyvalue.KeyValueStore
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

class KeyValueJobStatusCache(
    private val store: KeyValueStore,
    private val ttl: Duration = 5.minutes,
) : JobStatusCache {
    init {
        require(ttl.isFinite() && ttl.isPositive()) { "TTL must be positive and finite" }
    }

    override suspend fun get(
        ownerId: OwnerId,
        jobId: JobId,
    ): Job? =
        bestEffort {
            val value = store.get(key(ownerId, jobId)) ?: return@bestEffort null
            val status = Json.decodeFromString<CachedStatus>(value)
            val storyId = status.storyId?.let { StoryId.parse(it) ?: return@bestEffort null }
            Job(jobId, status.stage, storyId, status.failure)
        }

    override suspend fun put(
        ownerId: OwnerId,
        job: Job,
    ) {
        val stored =
            bestEffort {
                val status = CachedStatus(job.stage, job.storyId?.value, job.failure)
                store.put(key(ownerId, job.id), Json.encodeToString(status), ttl)
            }
        if (stored == null) invalidate(ownerId, job.id)
    }

    override suspend fun invalidate(
        ownerId: OwnerId,
        jobId: JobId,
    ) {
        bestEffort { store.delete(key(ownerId, jobId)) }
    }

    private fun key(
        ownerId: OwnerId,
        jobId: JobId,
    ) = "story:job-status:v1:${ownerId.value}:${jobId.value}"

    private suspend fun <T> bestEffort(action: suspend () -> T): T? =
        try {
            action()
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            null
        }

    @Serializable
    private data class CachedStatus(
        val stage: Stage,
        val storyId: String?,
        val failure: Failure?,
    )
}
