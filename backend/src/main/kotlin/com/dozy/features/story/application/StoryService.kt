package com.dozy.features.story.application

import com.dozy.domain.id.IdempotencyKey
import com.dozy.domain.id.JobId
import com.dozy.domain.id.LibraryCursor
import com.dozy.domain.id.OwnerId
import com.dozy.domain.id.StoryId
import com.dozy.features.story.domain.model.StartGeneration
import com.dozy.features.story.domain.repository.GenerationRepository

class StoryService(private val repository: GenerationRepository) {
    suspend fun list(installation: OwnerId, limit: Int, cursor: LibraryCursor?) = repository.list(installation, limit, cursor)
    suspend fun start(installation: OwnerId, key: IdempotencyKey, canonicalRequest: String, command: StartGeneration) = repository.start(installation, key, canonicalRequest, command)
    suspend fun retry(installation: OwnerId, id: JobId, key: IdempotencyKey) = repository.retry(installation, id, key)
    suspend fun status(installation: OwnerId, id: JobId) = repository.status(installation, id)
    suspend fun read(installation: OwnerId, id: StoryId) = repository.read(installation, id)
}
