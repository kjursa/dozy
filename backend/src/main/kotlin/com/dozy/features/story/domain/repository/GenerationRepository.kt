package com.dozy.features.story.domain.repository

import com.dozy.domain.id.IdempotencyKey
import com.dozy.domain.id.JobId
import com.dozy.domain.id.LibraryCursor
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

interface GenerationRepository {
    suspend fun list(installation: OwnerId, limit: Int, cursor: LibraryCursor?): StoryPage?
    suspend fun start(installation: OwnerId, key: IdempotencyKey, canonicalRequest: String, command: StartGeneration): Submission
    suspend fun retry(installation: OwnerId, id: JobId, key: IdempotencyKey): RetrySubmission
    suspend fun status(installation: OwnerId, id: JobId): Job?
    suspend fun read(installation: OwnerId, id: StoryId): SavedStory?
    suspend fun claim(): Execution?
    suspend fun renew(execution: Execution): Boolean
    suspend fun saveText(execution: Execution, text: GeneratedText): Boolean
    suspend fun publish(execution: Execution, cover: ByteArray): Boolean
    suspend fun fail(execution: Execution, failure: Failure)
}
