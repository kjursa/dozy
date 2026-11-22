package com.dozy.features.character.data.database

import com.dozy.domain.VersionedCollection
import com.dozy.domain.id.OwnerId
import com.dozy.features.character.domain.model.Character
import com.dozy.infrastructure.database.SqliteDatabase
import com.dozy.infrastructure.database.TrackedCollection
import com.dozy.infrastructure.database.collectionVersion
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll

class CharacterDao(
    private val database: SqliteDatabase,
) {
    suspend fun list(
        ownerId: OwnerId,
        isCurrent: (String) -> Boolean,
    ): VersionedCollection<Character> =
        database.write {
            val version = collectionVersion(TrackedCollection.CHARACTERS, ownerId.value)
            if (isCurrent(version)) return@write VersionedCollection(version, null)
            val characters =
                Characters
                    .selectAll()
                    .where {
                        (Characters.ownerId eq ownerId.value) and (Characters.deleted eq false)
                    }.map { it.toCharacter() }
            VersionedCollection(version, characters)
        }
}
