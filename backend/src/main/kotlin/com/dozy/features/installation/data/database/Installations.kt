package com.dozy.features.installation.data.database

import org.jetbrains.exposed.v1.core.Table

internal object Installations : Table("installations") {
    val id = text("id")
    override val primaryKey = PrimaryKey(id)
}
