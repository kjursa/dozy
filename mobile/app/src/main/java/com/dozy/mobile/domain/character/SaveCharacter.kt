package com.dozy.mobile.domain.character

import kotlinx.coroutines.CancellationException

class SaveCharacter(
    private val repository: CharacterRepository,
    private val ids: CharacterIdGenerator,
) {
    suspend operator fun invoke(draft: CharacterDraft): SaveCharacterResult =
        try {
            SaveCharacterResult.Success(repository.save(Character(ids.next(), draft)))
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            SaveCharacterResult.Failure
        }
}
