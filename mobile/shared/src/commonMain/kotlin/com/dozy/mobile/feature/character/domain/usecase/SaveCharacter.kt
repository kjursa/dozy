package com.dozy.mobile.feature.character.domain.usecase

import com.dozy.mobile.feature.character.domain.model.Character
import com.dozy.mobile.feature.character.domain.model.CreateCharacterInput
import com.dozy.mobile.feature.character.domain.model.EditCharacterInput
import com.dozy.mobile.feature.character.domain.model.SavedCharacter
import com.dozy.mobile.feature.character.domain.repository.CharacterRepository
import com.kjursa.foundation.coroutines.runCatchingSuspend

class SaveCharacter(
    private val repository: CharacterRepository,
) {
    suspend operator fun invoke(
        input: CreateCharacterInput,
        avatar: Character.Avatar,
        requestKey: String? = null,
    ): Result =
        runCatchingSuspend {
            repository.create(input, avatar, requestKey)
        }.fold(
            onSuccess = { Result.Success(it) },
            onFailure = { Result.Failure },
        )

    suspend operator fun invoke(
        input: EditCharacterInput,
        avatar: Character.Avatar,
    ): Result =
        runCatchingSuspend {
            val fields = input.fields
            repository.update(
                Character(input.id, Character.Details(fields.name, fields.personality, fields.appearance, fields.type), avatar),
            )
        }.fold(
            onSuccess = { if (it == null) Result.Unavailable else Result.Success(it) },
            onFailure = { Result.Failure },
        )

    sealed interface Result {
        data object Unavailable : Result

        data class Success(
            val saved: SavedCharacter,
        ) : Result

        data object Failure : Result
    }
}
