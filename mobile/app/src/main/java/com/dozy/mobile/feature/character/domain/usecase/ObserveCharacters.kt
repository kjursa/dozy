package com.dozy.mobile.feature.character.domain.usecase

import com.dozy.mobile.feature.character.domain.model.CharacterCollection
import com.dozy.mobile.feature.character.domain.repository.CharacterRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Both lists share the same durable source; Room emits again when a character is saved. */
class ObserveCharacters(
    private val repository: CharacterRepository,
) {
    operator fun invoke(): Flow<CharacterCollection> = repository.observe().map { CharacterCollection(it) }
}
