package com.dozy.mobile.feature.character.domain.usecase

import com.dozy.mobile.feature.character.domain.repository.CharacterRepository
import kotlinx.coroutines.CancellationException

class RefreshCharacters(
    private val repository: CharacterRepository,
) {
    suspend operator fun invoke(): Result =
        try {
            if (repository.refresh()) Result.Success else Result.Offline
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            Result.Failure
        }

    sealed interface Result {
        data object Success : Result

        data object Offline : Result

        data object Failure : Result
    }
}
