package com.dozy.features.story.data.repository

import com.dozy.domain.id.IdempotencyKey
import com.dozy.domain.id.JobId
import com.dozy.domain.id.OwnerId
import com.dozy.features.story.domain.model.Execution
import com.dozy.features.story.domain.model.Failure
import com.dozy.features.story.domain.model.GeneratedText
import com.dozy.features.story.domain.model.Job
import com.dozy.features.story.domain.model.RetrySubmission
import com.dozy.features.story.domain.model.Stage
import com.dozy.features.story.domain.model.StartGeneration
import com.dozy.features.story.domain.model.Submission
import com.dozy.features.story.domain.repository.GenerationRepository
import com.dozy.features.story.domain.repository.JobStatusCache
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class CachedGenerationRepository(
    private val repository: GenerationRepository,
    private val cache: JobStatusCache,
) : GenerationRepository by repository {
    // Order cache fills and status writes together so an older snapshot cannot replace a newer one.
    private val cacheMutex = Mutex()

    override suspend fun status(
        ownerId: OwnerId,
        id: JobId,
    ): Job? =
        cache.get(ownerId, id) ?: cacheMutex.withLock {
            cache.get(ownerId, id) ?: repository.status(ownerId, id)?.also { cache.put(ownerId, it) }
        }

    override suspend fun start(
        ownerId: OwnerId,
        key: IdempotencyKey,
        canonicalRequest: String,
        command: StartGeneration,
    ): Submission =
        completeTransition {
            repository.start(ownerId, key, canonicalRequest, command).also {
                if (it is Submission.Accepted) cache.put(ownerId, it.job)
            }
        }

    override suspend fun retry(
        ownerId: OwnerId,
        id: JobId,
        key: IdempotencyKey,
    ): RetrySubmission =
        completeTransition {
            repository.retry(ownerId, id, key).also {
                if (it is RetrySubmission.Accepted && !it.isReplay) cache.put(ownerId, it.job)
            }
        }

    override suspend fun claim(): Execution? =
        completeTransition {
            repository.claim()?.also { cache.put(it.ownerId, it.job) }
        }

    override suspend fun saveText(
        execution: Execution,
        text: GeneratedText,
    ): Boolean =
        completeTransition {
            repository.saveText(execution, text).also {
                if (it) cache.put(execution.ownerId, Job(execution.job.id, Stage.CREATING_COVER))
            }
        }

    override suspend fun publish(
        execution: Execution,
        cover: ByteArray,
    ): Boolean =
        completeTransition {
            repository.publish(execution, cover).also {
                if (it) cache.put(execution.ownerId, Job(execution.job.id, Stage.READY, storyId = execution.storyId))
            }
        }

    override suspend fun fail(
        execution: Execution,
        failure: Failure,
    ): Boolean =
        completeTransition {
            repository.fail(execution, failure).also {
                if (it) cache.put(execution.ownerId, Job(execution.job.id, Stage.FAILED, failure = failure))
            }
        }

    private suspend fun <T> completeTransition(action: suspend () -> T): T =
        cacheMutex.withLock {
            currentCoroutineContext().ensureActive()
            // SQLite can commit before a cancelled coroutine resumes from its IO dispatcher.
            withContext(NonCancellable) { action() }
        }
}
