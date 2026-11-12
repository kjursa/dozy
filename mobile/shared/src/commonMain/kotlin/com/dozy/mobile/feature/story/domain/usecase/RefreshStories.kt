package com.dozy.mobile.feature.story.domain.usecase

import com.dozy.mobile.feature.story.domain.repository.GenerationRepository
import com.kjursa.foundation.coroutines.runCatchingSuspend

class RefreshStories(
    private val repository: GenerationRepository,
) {
    suspend operator fun invoke(): Result =
        runCatchingSuspend {
            if (repository.refreshStories()) Result.Refreshed else Result.Offline
        }.getOrElse { Result.Failure }

    sealed interface Result {
        data object Refreshed : Result

        data object Offline : Result

        data object Failure : Result
    }
}
