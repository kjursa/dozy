package com.dozy.mobile.feature.character.domain.repository

import com.dozy.mobile.feature.character.domain.model.Character
import com.dozy.mobile.feature.character.domain.model.CharacterSync
import com.dozy.mobile.feature.character.domain.model.CreateCharacterInput
import com.dozy.mobile.feature.character.domain.model.SavedCharacter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface CharacterRepository {
    /** Null means generation is unavailable without a configured backend. */
    suspend fun generateAvatar(input: CreateCharacterInput): Character.Avatar? = null

    /** False means no backend is configured; failures throw without replacing known data. */
    suspend fun refresh(): Boolean = false

    fun observe(id: Character.Id): Flow<Character?> = observe().map { rows -> rows.firstOrNull { it.id == id } }

    suspend fun delete(id: Character.Id): CharacterSync

    suspend fun save(character: Character): SavedCharacter

    fun observe(): Flow<List<Character>>
}
