package com.dozy.features.character.data.repository

import com.dozy.features.asset.data.database.Assets
import com.dozy.features.character.data.database.Characters
import com.dozy.features.character.data.database.toCharacter
import com.dozy.features.character.domain.model.Character
import com.dozy.features.character.domain.model.SaveCharacter
import com.dozy.features.character.domain.model.SaveCharacterResult
import com.dozy.features.character.domain.repository.CharacterRepository
import com.dozy.features.installation.data.database.registerInstallation
import com.dozy.features.installation.domain.model.InstallationId
import com.dozy.infrastructure.database.SqliteDatabase
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import org.jetbrains.exposed.v1.jdbc.upsert

class SqliteCharacterRepository(private val database: SqliteDatabase) : CharacterRepository {
    override suspend fun list(installation: InstallationId): List<Character> = database.write {
        registerInstallation(installation)
        Characters.selectAll().where {
            (Characters.installationId eq installation.value) and (Characters.deleted eq false)
        }.map { it.toCharacter() }
    }

    override suspend fun save(installation: InstallationId, id: Character.Id, command: SaveCharacter): SaveCharacterResult = database.write {
        val existing = Characters.selectAll().where { Characters.id eq id.value }.singleOrNull()
        if (existing != null && (existing[Characters.installationId] != installation.value || existing[Characters.deleted])) {
            return@write SaveCharacterResult.Conflict
        }
        command.avatar?.let { avatar ->
            val available = Assets.selectAll().where {
                (Assets.id eq avatar.id) and (Assets.installationId eq installation.value) and
                    (Assets.url eq avatar.url) and (Assets.status eq "READY")
            }.any()
            if (!available) return@write SaveCharacterResult.ReferenceUnavailable
        }
        registerInstallation(installation)
        Characters.upsert(Characters.id) {
            it[Characters.id] = id.value
            it[installationId] = installation.value
            it[name] = command.name
            it[personality] = command.personality
            it[appearance] = command.appearance
            it[type] = command.type
            it[avatarId] = command.avatar?.id
            it[avatarUrl] = command.avatar?.url
            it[deleted] = false
        }
        if (existing == null) SaveCharacterResult.Created else SaveCharacterResult.Updated
    }

    override suspend fun delete(installation: InstallationId, id: Character.Id): Boolean = database.write {
        // Keep the row permanently: its ID can never be reused, including by its original owner.
        val existing = Characters.selectAll().where { Characters.id eq id.value }.singleOrNull()
        if (existing == null || existing[Characters.installationId] != installation.value) return@write false
        Characters.update({ Characters.id eq id.value }) { it[deleted] = true }
        true
    }
}
