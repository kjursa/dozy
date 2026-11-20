package com.dozy.features.story

import com.dozy.domain.id.IdempotencyKey
import com.dozy.domain.id.JobId
import com.dozy.domain.id.OwnerId
import com.dozy.domain.id.StoryId
import com.dozy.features.story.domain.model.Execution
import com.dozy.features.story.domain.model.Failure
import com.dozy.features.story.domain.model.GeneratedText
import com.dozy.features.story.domain.model.Job
import com.dozy.features.story.domain.model.RetrySubmission
import com.dozy.features.story.domain.model.SavedStory
import com.dozy.features.story.domain.model.StartGeneration
import com.dozy.features.story.domain.model.StoryPage
import com.dozy.features.story.domain.model.Submission
import com.dozy.features.story.domain.repository.GenerationRepository

/** Keeps unrelated route DI tests independent of background storage. */
class UnusedGenerationRepository : GenerationRepository {
    override suspend fun list(ownerId: OwnerId, limit: Int, cursor: StoryId?): StoryPage? = error("unused")
    override suspend fun claim(): Execution? = null
    override suspend fun start(ownerId: OwnerId, key: IdempotencyKey, canonicalRequest: String, command: StartGeneration): Submission = error("Unused")
    override suspend fun retry(ownerId: OwnerId, id: JobId, key: IdempotencyKey): RetrySubmission = error("Unused")
    override suspend fun status(ownerId: OwnerId, id: JobId): Job? = error("Unused")
    override suspend fun read(ownerId: OwnerId, id: StoryId): SavedStory? = error("Unused")
    override suspend fun renew(execution: Execution): Boolean = error("Unused")
    override suspend fun saveText(execution: Execution, text: GeneratedText): Boolean = error("Unused")
    override suspend fun publish(execution: Execution, cover: ByteArray): StoryId? = error("Unused")
    override suspend fun fail(execution: Execution, failure: Failure): Boolean = error("Unused")
}
