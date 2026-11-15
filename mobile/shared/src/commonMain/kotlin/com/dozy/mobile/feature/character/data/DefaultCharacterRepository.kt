package com.dozy.mobile.feature.character.data

import com.dozy.api.client.ApiDisabledException
import com.dozy.api.client.ApiResult
import com.dozy.api.client.character.CharactersClients
import com.dozy.api.client.characteravatar.CharacterAvatarsClients
import com.dozy.api.contracts.v1.characteravatar.AvatarJobState
import com.dozy.mobile.core.config.ApiConfig
import com.dozy.mobile.feature.character.data.database.CharacterDao
import com.dozy.mobile.feature.character.data.database.CharacterEntity
import com.dozy.mobile.feature.character.domain.model.Character
import com.dozy.mobile.feature.character.domain.model.CharacterSync
import com.dozy.mobile.feature.character.domain.model.CreateCharacterInput
import com.dozy.mobile.feature.character.domain.model.SavedCharacter
import com.dozy.mobile.feature.character.domain.repository.CharacterRepository
import com.kjursa.foundation.coroutines.runCatchingSuspend
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Local-first persistence; mutations and acknowledgements are serialized. */
class DefaultCharacterRepository(
    private val dao: CharacterDao,
    private val characters: CharactersClients,
    private val avatars: CharacterAvatarsClients,
    private val config: ApiConfig = ApiConfig(null),
) : CharacterRepository {
    private val mutations = Mutex()

    override suspend fun generateAvatar(input: CreateCharacterInput): Character.Avatar? {
        var job =
            try {
                when (val result = avatars.v1.start(input.toAvatarDto())) {
                    is ApiResult.Success -> result.value
                    is ApiResult.Failure -> error("Avatar submission failed")
                }
            } catch (_: ApiDisabledException) {
                return null
            }
        val id = job.jobId
        repeat(121) { poll ->
            check(job.jobId == id) { "Mismatched avatar job" }
            when (job.status) {
                AvatarJobState.READY.name -> return requireNotNull(job.asset).toDomain(config.allowLocalAssetHttp)
                AvatarJobState.FAILED.name -> error("Avatar generation failed")
                AvatarJobState.QUEUED.name, AvatarJobState.RUNNING.name -> Unit
                else -> error("Unknown avatar state")
            }
            check(poll < 120) { "Avatar generation timed out" }
            delay(1000)
            job =
                when (val result = avatars.v1.status(id)) {
                    is ApiResult.Success -> result.value
                    is ApiResult.Failure -> error("Avatar status unavailable")
                }
        }
        error("Avatar generation timed out")
    }

    override fun observe() = dao.observe().map { rows -> rows.map { it.toDomain() } }

    override fun observe(id: Character.Id) = dao.observe(id.value).map { it?.toDomain() }

    override suspend fun delete(id: Character.Id): CharacterSync =
        mutations.withLock {
            dao.markDeleted(id.value)
            synchronizeDeletion(id.value)
        }

    private suspend fun synchronizeDeletion(id: String): CharacterSync =
        if (runCatchingSuspend { characters.v1.delete(id) is ApiResult.Success }.getOrDefault(false)) {
            CharacterSync.SYNCED
        } else {
            CharacterSync.PENDING
        }

    override suspend fun refresh(): Boolean =
        mutations.withLock {
            // Retain tombstones even after acknowledgement to reject stale server snapshots.
            dao.deletedIds().forEach { synchronizeDeletion(it) }
            dao
                .observe()
                .first()
                .filter { it.pendingSync }
                .forEach { synchronize(it) }
            val result =
                try {
                    characters.v1.list()
                } catch (_: ApiDisabledException) {
                    return@withLock false
                }
            val snapshot =
                when (result) {
                    is ApiResult.Success -> result.value
                    is ApiResult.Failure -> return@withLock false
                }
            require(snapshot.map { it.id }.distinct().size == snapshot.size) { "Duplicate character IDs" }
            val entities = snapshot.map { it.toDomain(config.allowLocalAssetHttp).toEntity(pendingSync = false) }
            dao.replaceSnapshot(entities)
            true
        }

    override suspend fun update(character: Character): SavedCharacter? =
        mutations.withLock {
            val row = character.toEntity(pendingSync = true)
            val changed = dao.updateExisting(row.id, row.name, row.personality, row.appearance, row.type, row.avatarId, row.avatarUrl)
            if (changed == 0) return@withLock null
            SavedCharacter(character, synchronize(row))
        }

    override suspend fun save(character: Character): SavedCharacter =
        mutations.withLock {
            require(character.id.value !in dao.deletedIds()) { "Deleted character ID cannot be reused" }
            val row = character.toEntity(pendingSync = true)
            dao.upsert(row)
            SavedCharacter(character, synchronize(row))
        }

    private suspend fun synchronize(row: CharacterEntity): CharacterSync {
        // Bundled previews have no server asset metadata and must remain local-only.
        if (row.avatarId?.startsWith("fake:") == true || row.avatarUrl?.startsWith("bundled-avatar:") == true) {
            return CharacterSync.PENDING
        }
        val acknowledged =
            runCatchingSuspend {
                characters.v1.save(row.id, row.toDomain().toSaveRequest()) is ApiResult.Success
            }.getOrDefault(false)
        if (!acknowledged) return CharacterSync.PENDING
        return if (runCatchingSuspend { dao.upsert(row.copy(pendingSync = false)) }.isSuccess) {
            CharacterSync.SYNCED
        } else {
            CharacterSync.PENDING
        }
    }
}
