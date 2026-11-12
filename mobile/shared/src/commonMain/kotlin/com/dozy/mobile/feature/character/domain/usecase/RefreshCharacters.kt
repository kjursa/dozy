package com.dozy.mobile.feature.character.domain.usecase

import com.dozy.mobile.feature.character.domain.repository.CharacterRepository
import com.kjursa.foundation.coroutines.runCatchingSuspend

class RefreshCharacters(
    private val repository: CharacterRepository,
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
