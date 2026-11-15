package com.dozy.features.character.data.database

import com.dozy.features.asset.data.database.Assets
import com.dozy.features.character.domain.model.Character
import com.dozy.features.installation.data.database.Installations
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.ResultRow

internal object Characters : Table("characters") {
    val id = text("id")
    val installationId = reference("installation_id", Installations.id)
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
    Character.Id(this[Characters.id]), this[Characters.name], this[Characters.personality],
    this[Characters.appearance], this[Characters.type],
    this[Characters.avatarId]?.let { Character.Avatar(it, requireNotNull(this[Characters.avatarUrl])) },
)
