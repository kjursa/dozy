package com.dozy.mobile.feature.character.domain.repository

import com.dozy.mobile.feature.character.domain.model.Character
import com.dozy.mobile.feature.character.domain.model.CreateCharacterInput
import com.dozy.mobile.feature.character.domain.model.SavedCharacter
import kotlinx.coroutines.flow.Flow

interface CharacterRepository {
    /** Null means generation is unavailable without a configured backend. */
    suspend fun generateAvatar(input: CreateCharacterInput): Character.Avatar? = null

    suspend fun save(character: Character): SavedCharacter

    fun observe(): Flow<List<Character>>
}
