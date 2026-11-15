package com.dozy.features.asset.data.database

import com.dozy.features.installation.data.database.Installations
import org.jetbrains.exposed.v1.core.Table

internal object Assets : Table("assets") {
    val id = text("id")
    val installationId = reference("installation_id", Installations.id)
    val url = text("url")
    val status = text("status")
    override val primaryKey = PrimaryKey(id)
}
