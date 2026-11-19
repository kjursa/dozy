package com.dozy.features.story.data.repository

import com.dozy.domain.id.IdempotencyKey
import com.dozy.domain.id.JobId
import com.dozy.domain.id.OwnerId
import com.dozy.features.story.domain.model.Execution
import com.dozy.features.story.domain.model.Failure
import com.dozy.features.story.domain.model.GeneratedText
import com.dozy.features.story.domain.model.Job
import com.dozy.features.story.domain.model.RetrySubmission
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
    // Keep a cache miss from restoring an older status after a committed write invalidates it.
    private val cacheMutex = Mutex()

    override suspend fun status(
        ownerId: OwnerId,
        id: JobId,
    ): Job? =
        cache.get(ownerId, id) ?: cacheMutex.withLock {
            cache.get(ownerId, id) ?: repository.status(ownerId, id)?.also { cache.put(ownerId, it) }
        }

    override suspend fun retry(
        ownerId: OwnerId,
        id: JobId,
        key: IdempotencyKey,
    ): RetrySubmission =
        completeTransition {
            repository.retry(ownerId, id, key).also { if (it is RetrySubmission.Accepted) invalidate(ownerId, id) }
        }

    override suspend fun claim(): Execution? =
        completeTransition {
            repository.claim()?.also { invalidate(it.ownerId, it.job.id) }
        }

    override suspend fun saveText(
        execution: Execution,
        text: GeneratedText,
    ): Boolean =
        completeTransition {
            repository.saveText(execution, text).also { if (it) invalidate(execution.ownerId, execution.job.id) }
        }

    override suspend fun publish(
        execution: Execution,
        cover: ByteArray,
    ): Boolean =
        completeTransition {
            repository.publish(execution, cover).also { if (it) invalidate(execution.ownerId, execution.job.id) }
        }

    override suspend fun fail(
        execution: Execution,
        failure: Failure,
    ) = completeTransition {
        repository.fail(execution, failure)
        invalidate(execution.ownerId, execution.job.id)
    }

    private suspend fun invalidate(
        ownerId: OwnerId,
        jobId: JobId,
    ) {
        cacheMutex.withLock { cache.invalidate(ownerId, jobId) }
    }

    private suspend fun <T> completeTransition(action: suspend () -> T): T {
        currentCoroutineContext().ensureActive()
        // SQLite can commit before a cancelled coroutine resumes from its IO dispatcher.
        return withContext(NonCancellable) { action() }
    }
}
