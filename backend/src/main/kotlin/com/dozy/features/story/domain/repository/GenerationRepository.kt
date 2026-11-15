package com.dozy.features.story.domain.repository

import com.dozy.features.installation.domain.model.InstallationId
import com.dozy.features.story.domain.model.RetrySubmission
import com.dozy.features.story.domain.model.Execution
import com.dozy.features.story.domain.model.Failure
import com.dozy.features.story.domain.model.GeneratedText
import com.dozy.features.story.domain.model.Job
import com.dozy.features.story.domain.model.SavedStory
import com.dozy.features.story.domain.model.StartGeneration
import com.dozy.features.story.domain.model.Submission

interface GenerationRepository {
    suspend fun start(installation: InstallationId, key: String, canonicalRequest: String, command: StartGeneration): Submission
    suspend fun retry(installation: InstallationId, id: String, key: String): RetrySubmission
    suspend fun status(installation: InstallationId, id: String): Job?
    suspend fun read(installation: InstallationId, id: String): SavedStory?
    suspend fun claim(): Execution?
    suspend fun renew(execution: Execution): Boolean
    suspend fun saveText(execution: Execution, text: GeneratedText): Boolean
    suspend fun publish(execution: Execution, cover: ByteArray): Boolean
    suspend fun fail(execution: Execution, failure: Failure)
}
