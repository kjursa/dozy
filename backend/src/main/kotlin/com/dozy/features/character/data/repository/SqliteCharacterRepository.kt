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
import com.dozy.infrastructure.database.ResourceCreation
import com.dozy.infrastructure.database.SqliteDatabase
import com.dozy.infrastructure.id.Uuids
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update

class SqliteCharacterRepository(private val database: SqliteDatabase) : CharacterRepository {
    override suspend fun list(installation: InstallationId): List<Character> = database.write {
        registerInstallation(installation)
        Characters.selectAll().where {
            (Characters.installationId eq installation.value) and (Characters.deleted eq false)
        }.map { it.toCharacter() }
    }

    override suspend fun create(
        installation: InstallationId,
        command: SaveCharacter,
        requestKey: String?
    ): SaveCharacterResult = database.write {
        val creation = requestKey?.let {
            ResourceCreation(
                installation,
                "character",
                it,
                listOf(
                    command.name,
                    command.personality,
                    command.appearance,
                    command.type.name,
                    command.avatar?.id,
                    command.avatar?.url
                )
            )
        }
        creation?.previous()?.let {
            if (!it.sameInput) return@write SaveCharacterResult.IdempotencyConflict
            val previous = Characters.selectAll().where { Characters.id eq it.id }.single()
            if (previous[Characters.deleted]) return@write SaveCharacterResult.Conflict
            return@write SaveCharacterResult.Created(Character.Id(it.id))
        }
        if (!avatarAvailable(
                installation,
                command.avatar
            )
        ) {
            return@write SaveCharacterResult.ReferenceUnavailable
        }
        val id = Character.Id(Uuids.v7())
        registerInstallation(installation)
        Characters.insert {
            it[Characters.id] = id.value
            it[installationId] = installation.value
            it.setFields(command)
        }
        creation?.record(id.value)
        SaveCharacterResult.Created(id)
    }

    override suspend fun save(installation: InstallationId, id: Character.Id, command: SaveCharacter): SaveCharacterResult = database.write {
        val existing = Characters.selectAll().where { Characters.id eq id.value }.singleOrNull()
            ?: return@write SaveCharacterResult.NotFound
        if (existing[Characters.installationId] != installation.value ||
            existing[Characters.deleted]
        ) {
            return@write SaveCharacterResult.Conflict
        }
        if (!avatarAvailable(
                installation,
                command.avatar
            )
        ) {
            return@write SaveCharacterResult.ReferenceUnavailable
        }
        Characters.update({ Characters.id eq id.value }) { it.setFields(command) }
        SaveCharacterResult.Updated
    }

    private fun JdbcTransaction.avatarAvailable(
        installation: InstallationId,
        avatar: Character.Avatar?
    ): Boolean = avatar == null || Assets.selectAll().where {
        (Assets.id eq avatar.id) and (Assets.installationId eq installation.value) and
            (Assets.url eq avatar.url) and (Assets.status eq "READY")
    }.any()

    private fun UpdateBuilder<*>.setFields(command: SaveCharacter) {
        this[Characters.name] = command.name
        this[Characters.personality] = command.personality
        this[Characters.appearance] = command.appearance
        this[Characters.type] = command.type
        this[Characters.avatarId] = command.avatar?.id
        this[Characters.avatarUrl] = command.avatar?.url
    }

    override suspend fun delete(installation: InstallationId, id: Character.Id): Boolean = database.write {
        // Keep the row permanently: its ID can never be reused, including by its original owner.
        val existing = Characters.selectAll().where { Characters.id eq id.value }.singleOrNull()
        if (existing == null || existing[Characters.installationId] != installation.value) return@write false
        Characters.update({ Characters.id eq id.value }) { it[deleted] = true }
        true
    }
}
