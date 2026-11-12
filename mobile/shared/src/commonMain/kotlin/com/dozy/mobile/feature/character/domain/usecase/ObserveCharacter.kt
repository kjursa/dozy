package com.dozy.mobile.feature.character.domain.usecase

import com.dozy.mobile.feature.character.domain.model.Character
import com.dozy.mobile.feature.character.domain.repository.CharacterRepository

class ObserveCharacter(
    private val repository: CharacterRepository,
) {
    operator fun invoke(id: Character.Id) = repository.observe(id)
}
