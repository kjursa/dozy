package com.dozy.mobile.feature.story.domain.usecase

import com.dozy.mobile.feature.story.domain.model.GenerationJob
import com.dozy.mobile.feature.story.domain.repository.GenerationRepository
import com.kjursa.foundation.coroutines.runCatchingSuspend

class RefreshGeneration(
    private val repository: GenerationRepository,
) {
    suspend operator fun invoke(id: GenerationJob.Id): Result =
        runCatchingSuspend {
            repository.refresh(id)?.let { Result.Updated(it) } ?: Result.Unavailable
        }.getOrDefault(Result.Unavailable)

    sealed interface Result {
        data class Updated(
            val job: GenerationJob,
        ) : Result

        data object Unavailable : Result
    }
}
