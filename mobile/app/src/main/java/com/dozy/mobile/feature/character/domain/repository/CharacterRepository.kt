package com.dozy.mobile.feature.character.domain.repository

import com.dozy.mobile.feature.character.domain.model.Character
import com.dozy.mobile.feature.character.domain.model.SavedCharacter
import kotlinx.coroutines.flow.Flow

interface CharacterRepository {
    suspend fun save(character: Character): SavedCharacter

    fun observe(): Flow<List<Character>>
}
