package com.dozy.mobile.feature.place.domain.usecase

import com.dozy.mobile.feature.place.domain.repository.PlaceRepository
import com.kjursa.foundation.coroutines.runCatchingSuspend

class RefreshPlaces(
    private val repository: PlaceRepository,
) {
    suspend operator fun invoke(): Result =
        runCatchingSuspend {
            if (repository.refresh()) Result.Success else Result.Offline
        }.getOrDefault(Result.Failure)

    sealed interface Result {
        data object Success : Result

        data object Offline : Result

        data object Failure : Result
    }
}
