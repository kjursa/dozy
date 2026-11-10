package com.dozy.mobile.feature.character.domain

import com.kjursa.foundation.coroutines.runCatchingSuspend

class SaveCharacter(
    private val repository: CharacterRepository,
    private val ids: CharacterIdGenerator,
) {
    suspend operator fun invoke(input: CreateCharacterInput): SaveCharacterResult =
        runCatchingSuspend {
            repository.save(Character(ids.next(), Character.Details(input.name, input.personality, input.appearance, input.type)))
        }.fold(
            onSuccess = { SaveCharacterResult.Success(it) },
            onFailure = { SaveCharacterResult.Failure },
        )
}
