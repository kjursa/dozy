package com.dozy.mobile.feature.character.data

import com.dozy.mobile.core.coroutines.runCatchingSuspend
import com.dozy.mobile.feature.character.data.api.CharacterApi
import com.dozy.mobile.feature.character.data.database.CharacterDao
import com.dozy.mobile.feature.character.domain.Character
import com.dozy.mobile.feature.character.domain.CharacterRepository
import com.dozy.mobile.feature.character.domain.CharacterSync
import com.dozy.mobile.feature.character.domain.SavedCharacter
import kotlinx.coroutines.flow.map

/** Local-first: a successful save always means a durable row, even with no server. */
class DefaultCharacterRepository(
    private val dao: CharacterDao,
    private val api: CharacterApi,
) : CharacterRepository {
    override fun observe() = dao.observe().map { rows -> rows.map { it.toDomain() } }

    override suspend fun save(character: Character): SavedCharacter {
        // Fail before HTTP if the device cannot persist. Never report a non-durable save as success.
        dao.upsert(character.toEntity(pendingSync = true))
        val acknowledged = runCatchingSuspend { api.save(character.toDto()) }.getOrDefault(false)
        if (acknowledged) {
            val metadataSaved = runCatchingSuspend { dao.upsert(character.toEntity(pendingSync = false)) }
            if (metadataSaved.isFailure) {
                // The original row is already durable; do not encourage duplicate submissions.
                return SavedCharacter(character, CharacterSync.PENDING)
            }
        }
        return SavedCharacter(character, if (acknowledged) CharacterSync.SYNCED else CharacterSync.PENDING)
    }
}
