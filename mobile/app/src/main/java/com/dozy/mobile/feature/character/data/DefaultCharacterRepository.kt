package com.dozy.mobile.feature.character.data

import com.dozy.mobile.feature.character.data.api.CharacterApi
import com.dozy.mobile.feature.character.data.database.CharacterDao
import com.dozy.mobile.feature.character.domain.model.Character
import com.dozy.mobile.feature.character.domain.model.CharacterSync
import com.dozy.mobile.feature.character.domain.model.CreateCharacterInput
import com.dozy.mobile.feature.character.domain.model.SavedCharacter
import com.dozy.mobile.feature.character.domain.repository.CharacterRepository
import com.kjursa.foundation.coroutines.runCatchingSuspend
import kotlinx.coroutines.flow.map

/** Local-first: a successful save always means a durable row, even with no server. */
class DefaultCharacterRepository(
    private val dao: CharacterDao,
    private val api: CharacterApi,
) : CharacterRepository {
    override suspend fun generateAvatar(input: CreateCharacterInput) = api.generateAvatar(input.toAvatarDto())?.toDomain()

    override fun observe() = dao.observe().map { rows -> rows.map { it.toDomain() } }

    override fun observe(id: Character.Id) = dao.observe(id.value).map { it?.toDomain() }

    override suspend fun delete(id: Character.Id): CharacterSync {
        val acknowledged = api.delete(id.value)
        dao.markDeleted(id.value)
        return if (acknowledged) CharacterSync.SYNCED else CharacterSync.PENDING
    }

    override suspend fun refresh(): Boolean {
        // Tombstones survive acknowledgements to guard against stale snapshots.
        for (id in dao.deletedIds()) api.delete(id)
        val snapshot = api.list() ?: return false
        require(snapshot.map { it.id }.distinct().size == snapshot.size) { "Duplicate character IDs" }
        val entities = snapshot.map { it.toDomain().toEntity(pendingSync = false) }
        dao.replaceSnapshot(entities)
        return true
    }

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
