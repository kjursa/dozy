package com.dozy.features.character.data.database

import com.dozy.domain.id.AssetId
import com.dozy.domain.id.CharacterId
import com.dozy.features.asset.data.database.Assets
import com.dozy.features.character.domain.model.Character
import com.dozy.infrastructure.database.OwnerPartitions
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.Table

internal object Characters : Table("characters") {
    val id = text("id")
    val ownerId = reference("owner_id", OwnerPartitions.id)
    val name = text("name")
    val personality = text("personality")
    val appearance = text("appearance")
    val type = enumerationByName<Character.Type>("type", 16)
    val avatarId = reference("avatar_id", Assets.id).nullable()
    val avatarUrl = text("avatar_url").nullable()
    val deleted = bool("deleted").default(false)
    override val primaryKey = PrimaryKey(id)
}

internal fun ResultRow.toCharacter() = Character(
    requireNotNull(CharacterId.parse(this[Characters.id])), this[Characters.name], this[Characters.personality],
    this[Characters.appearance], this[Characters.type],
    this[Characters.avatarId]?.let { Character.Avatar(requireNotNull(AssetId.parse(it)), requireNotNull(this[Characters.avatarUrl])) },
)
