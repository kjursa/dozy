package com.dozy.features.story.data.database

import org.jetbrains.exposed.v1.core.Table

internal object LibrarySnapshots : Table("library_snapshots") {
    val id = text("id")
    val ownerId = text("installation_id")
    val limit = integer("page_limit")
    val summaries = text("summaries")
    override val primaryKey = PrimaryKey(id)
}
internal object LibraryCursors : Table("library_cursors") {
    val token = text("token")
    val snapshot = text("snapshot_id")
    val position = integer("position")
    override val primaryKey = PrimaryKey(token)
}
