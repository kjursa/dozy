package com.dozy.mobile.feature.character.domain.usecase

import com.dozy.mobile.feature.character.domain.model.Character
import com.dozy.mobile.feature.character.domain.repository.CharacterRepository

class DeleteCharacter(
    private val repository: CharacterRepository,
) {
    suspend operator fun invoke(id: Character.Id): Result =
        try {
            Result.Deleted(repository.delete(id))
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            Result.Failure
        }

    sealed interface Result {
        data class Deleted(
            val sync: com.dozy.mobile.feature.character.domain.model.CharacterSync,
        ) : Result

        data object Failure : Result
    }
}
