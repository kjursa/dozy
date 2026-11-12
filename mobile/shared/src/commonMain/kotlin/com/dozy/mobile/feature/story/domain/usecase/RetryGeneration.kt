package com.dozy.mobile.feature.story.domain.usecase

import com.dozy.mobile.feature.story.domain.model.GenerationJob
import com.dozy.mobile.feature.story.domain.repository.GenerationRepository
import com.kjursa.foundation.coroutines.runCatchingSuspend

class RetryGeneration(
    private val repository: GenerationRepository,
    private val keys: GenerationKeyGenerator,
) {
    suspend operator fun invoke(id: GenerationJob.Id): Result =
        runCatchingSuspend {
            repository.retry(id, keys.next())?.let { Result.Accepted(it) } ?: Result.Unavailable
        }.getOrDefault(Result.Unavailable)

    sealed interface Result {
        data class Accepted(
            val job: GenerationJob,
        ) : Result

        data object Unavailable : Result
    }
}
