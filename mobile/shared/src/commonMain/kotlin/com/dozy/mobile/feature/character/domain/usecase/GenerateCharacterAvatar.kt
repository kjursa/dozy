package com.dozy.mobile.feature.character.domain.usecase

import com.dozy.mobile.feature.character.domain.model.Character
import com.dozy.mobile.feature.character.domain.model.CreateCharacterInput
import com.dozy.mobile.feature.character.domain.repository.CharacterRepository
import com.kjursa.foundation.coroutines.runCatchingSuspend

class GenerateCharacterAvatar(
    private val repository: CharacterRepository,
) {
    suspend operator fun invoke(input: CreateCharacterInput): Result =
        runCatchingSuspend { repository.generateAvatar(input) }.fold(
            onSuccess = { if (it == null) Result.Unavailable else Result.Success(it) },
            onFailure = { Result.Failure },
        )

    sealed interface Result {
        data class Success(
            val avatar: Character.Avatar,
        ) : Result

        data object Unavailable : Result

        data object Failure : Result
    }
}
