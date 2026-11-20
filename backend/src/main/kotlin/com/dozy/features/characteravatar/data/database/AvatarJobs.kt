package com.dozy.features.characteravatar.data.database

import com.dozy.infrastructure.database.OwnerPartitions
import org.jetbrains.exposed.v1.core.Table

internal object Jobs : Table("avatar_jobs") {
    val id = text("id")
    val ownerId = reference("owner_id", OwnerPartitions.id)
    val type = text("type")
    val personality = text("personality")
    val appearance = text("appearance")
    val assetId = text("asset_id")
    val url = text("url")
    val state = text("state")
    override val primaryKey = PrimaryKey(id)
}
