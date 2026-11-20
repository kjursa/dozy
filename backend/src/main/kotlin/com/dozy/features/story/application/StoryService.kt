package com.dozy.features.story.application

import com.dozy.domain.id.IdempotencyKey
import com.dozy.domain.id.JobId
import com.dozy.domain.id.OwnerId
import com.dozy.domain.id.StoryId
import com.dozy.features.story.domain.model.GenerationStatus
import com.dozy.features.story.domain.model.StartGeneration
import com.dozy.features.story.domain.repository.GenerationRepository

class StoryService(private val repository: GenerationRepository) {
    suspend fun list(ownerId: OwnerId, limit: Int, cursor: StoryId?) = repository.list(ownerId, limit, cursor)
    suspend fun start(ownerId: OwnerId, key: IdempotencyKey, canonicalRequest: String, command: StartGeneration) = repository.start(ownerId, key, canonicalRequest, command)
    suspend fun retry(ownerId: OwnerId, id: JobId, key: IdempotencyKey) = repository.retry(ownerId, id, key)
    suspend fun status(ownerId: OwnerId, id: JobId): GenerationStatus? {
        val job = repository.status(ownerId, id) ?: return null
        val story = job.storyId?.let { checkNotNull(repository.read(ownerId, it)) }
        return GenerationStatus(job, story)
    }
    suspend fun read(ownerId: OwnerId, id: StoryId) = repository.read(ownerId, id)
}
