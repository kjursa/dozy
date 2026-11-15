package com.dozy.features.story

import com.dozy.features.installation.domain.model.InstallationId
import com.dozy.features.story.domain.model.RetrySubmission
import com.dozy.features.story.domain.model.Execution
import com.dozy.features.story.domain.model.Failure
import com.dozy.features.story.domain.model.GeneratedText
import com.dozy.features.story.domain.model.Job
import com.dozy.features.story.domain.model.SavedStory
import com.dozy.features.story.domain.model.StartGeneration
import com.dozy.features.story.domain.model.Submission
import com.dozy.features.story.domain.repository.GenerationRepository

/** Keeps unrelated route DI tests independent of background storage. */
class UnusedGenerationRepository : GenerationRepository {
    override suspend fun claim(): Execution? = null
    override suspend fun start(installation: InstallationId, key: String, canonicalRequest: String, command: StartGeneration): Submission = error("Unused")
    override suspend fun retry(installation: InstallationId, id: String, key: String): RetrySubmission = error("Unused")
    override suspend fun status(installation: InstallationId, id: String): Job? = error("Unused")
    override suspend fun read(installation: InstallationId, id: String): SavedStory? = error("Unused")
    override suspend fun renew(execution: Execution): Boolean = error("Unused")
    override suspend fun saveText(execution: Execution, text: GeneratedText): Boolean = error("Unused")
    override suspend fun publish(execution: Execution, cover: ByteArray): Boolean = error("Unused")
    override suspend fun fail(execution: Execution, failure: Failure): Unit = error("Unused")
}
