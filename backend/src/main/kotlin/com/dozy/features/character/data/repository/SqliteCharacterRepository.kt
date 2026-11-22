package com.dozy.features.character.data.repository

import com.dozy.domain.VersionedCollection
import com.dozy.domain.id.CharacterId
import com.dozy.domain.id.IdempotencyKey
import com.dozy.domain.id.OwnerId
import com.dozy.features.asset.data.database.Assets
import com.dozy.features.character.data.database.Characters
import com.dozy.features.character.data.database.toCharacter
import com.dozy.features.character.domain.model.Character
import com.dozy.features.character.domain.model.SaveCharacter
import com.dozy.features.character.domain.model.SaveCharacterResult
import com.dozy.features.character.domain.repository.CharacterRepository
import com.dozy.infrastructure.database.ResourceCreation
import com.dozy.infrastructure.database.SqliteDatabase
import com.dozy.infrastructure.database.TrackedCollection
import com.dozy.infrastructure.database.collectionVersion
import com.dozy.infrastructure.database.registerOwnerPartition
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update

class SqliteCharacterRepository(private val database: SqliteDatabase) : CharacterRepository {
    override suspend fun list(
        ownerId: OwnerId,
        isCurrent: (String) -> Boolean,
    ): VersionedCollection<Character> = database.write {
        val version = collectionVersion(TrackedCollection.CHARACTERS, ownerId.value)
        if (isCurrent(version)) return@write VersionedCollection(version, null)
        val characters = Characters.selectAll().where {
            (Characters.ownerId eq ownerId.value) and (Characters.deleted eq false)
        }.map { it.toCharacter() }
        VersionedCollection(version, characters)
    }

    override suspend fun create(
        ownerId: OwnerId,
        command: SaveCharacter,
        requestKey: IdempotencyKey?
    ): SaveCharacterResult = database.write {
        val creation = requestKey?.let {
            ResourceCreation(
                ownerId,
                "character",
                it,
                listOf(
                    command.name,
                    command.personality,
                    command.appearance,
                    command.type.name,
                    command.avatar?.id?.value,
                    command.avatar?.url
                )
            )
        }
        creation?.previous()?.let {
            if (!it.sameInput) return@write SaveCharacterResult.IdempotencyConflict
            val previous = Characters.selectAll().where { Characters.id eq it.id }.single()
            if (previous[Characters.deleted]) return@write SaveCharacterResult.Conflict
            return@write SaveCharacterResult.Created(requireNotNull(CharacterId.parse(it.id)))
        }
        if (!avatarAvailable(
                ownerId,
                command.avatar
            )
        ) {
            return@write SaveCharacterResult.ReferenceUnavailable
        }
        val id = CharacterId.generate()
        registerOwnerPartition(ownerId)
        Characters.insert {
            it[Characters.id] = id.value
            it[Characters.ownerId] = ownerId.value
            it.setFields(command)
        }
        creation?.record(id.value)
        SaveCharacterResult.Created(id)
    }

    override suspend fun save(ownerId: OwnerId, id: CharacterId, command: SaveCharacter): SaveCharacterResult = database.write {
        val existing = Characters.selectAll().where { Characters.id eq id.value }.singleOrNull()
            ?: return@write SaveCharacterResult.NotFound
        if (existing[Characters.ownerId] != ownerId.value ||
            existing[Characters.deleted]
        ) {
            return@write SaveCharacterResult.Conflict
        }
        if (!avatarAvailable(
                ownerId,
                command.avatar
            )
        ) {
            return@write SaveCharacterResult.ReferenceUnavailable
        }
        Characters.update({ Characters.id eq id.value }) { it.setFields(command) }
        SaveCharacterResult.Updated
    }

    private fun JdbcTransaction.avatarAvailable(
        ownerId: OwnerId,
        avatar: Character.Avatar?
    ): Boolean = avatar == null || Assets.selectAll().where {
        (Assets.id eq avatar.id.value) and (Assets.ownerId eq ownerId.value) and
            (Assets.url eq avatar.url) and (Assets.status eq "READY")
    }.any()

    private fun UpdateBuilder<*>.setFields(command: SaveCharacter) {
        this[Characters.name] = command.name
        this[Characters.personality] = command.personality
        this[Characters.appearance] = command.appearance
        this[Characters.type] = command.type
        this[Characters.avatarId] = command.avatar?.id?.value
        this[Characters.avatarUrl] = command.avatar?.url
    }

    override suspend fun delete(ownerId: OwnerId, id: CharacterId): Boolean = database.write {
        // Keep the row permanently: its ID can never be reused, including by its original owner.
        val existing = Characters.selectAll().where { Characters.id eq id.value }.singleOrNull()
        if (existing == null || existing[Characters.ownerId] != ownerId.value) return@write false
        Characters.update({ Characters.id eq id.value }) { it[deleted] = true }
        true
    }
}
