package com.dozy.mobile.domain.character

import com.dozy.mobile.core.coroutines.runCatchingSuspend

class SaveCharacter(
    private val repository: CharacterRepository,
    private val ids: CharacterIdGenerator,
) {
    suspend operator fun invoke(draft: CharacterDraft): SaveCharacterResult =
        runCatchingSuspend {
            repository.save(Character(ids.next(), draft))
        }.fold(
            onSuccess = { SaveCharacterResult.Success(it) },
            onFailure = { SaveCharacterResult.Failure },
        )
}
