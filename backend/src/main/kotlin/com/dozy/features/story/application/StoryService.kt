package com.dozy.features.story.application

import com.dozy.features.installation.domain.model.InstallationId
import com.dozy.features.story.domain.model.StartGeneration
import com.dozy.features.story.domain.repository.GenerationRepository

class StoryService(private val repository: GenerationRepository) {
    suspend fun start(installation: InstallationId, key: String, canonicalRequest: String, command: StartGeneration) = repository.start(installation, key, canonicalRequest, command)
    suspend fun retry(installation: InstallationId, id: String, key: String) = repository.retry(installation, id, key)
    suspend fun status(installation: InstallationId, id: String) = repository.status(installation, id)
    suspend fun read(installation: InstallationId, id: String) = repository.read(installation, id)
}
