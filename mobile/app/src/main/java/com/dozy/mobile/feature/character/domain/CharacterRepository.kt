package com.dozy.mobile.feature.character.domain

import kotlinx.coroutines.flow.Flow

interface CharacterRepository {
    suspend fun save(character: Character): SavedCharacter

    fun observe(): Flow<List<Character>>
}
