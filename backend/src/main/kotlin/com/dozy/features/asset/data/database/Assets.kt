package com.dozy.features.asset.data.database

import com.dozy.infrastructure.database.OwnerPartitions
import org.jetbrains.exposed.v1.core.Table

internal object Assets : Table("assets") {
    val id = text("id")
    val ownerId = reference("installation_id", OwnerPartitions.id)
    val url = text("url")
    val status = text("status")
    override val primaryKey = PrimaryKey(id)
}
