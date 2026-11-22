package com.dozy.infrastructure.database

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import java.util.UUID

internal const val SYSTEM_CATALOG_OWNER = ""

private object CollectionEtags : Table("collection_etags") {
    val collection = text("collection")
    val owner = text("owner_id")
    val version = text("version")
    override val primaryKey = PrimaryKey(collection, owner)
}

internal enum class TrackedCollection(
    val key: String,
) {
    CHARACTERS("characters"),
    PLACES("places"),
}

internal fun JdbcTransaction.collectionVersion(
    collection: TrackedCollection,
    ownerId: String,
): String {
    val existing =
        CollectionEtags
            .select(CollectionEtags.version)
            .where { (CollectionEtags.collection eq collection.key) and (CollectionEtags.owner eq ownerId) }
            .singleOrNull()
    if (existing != null) return existing[CollectionEtags.version]

    // New accounts with empty collections also receive a stable validator.
    val version = UUID.randomUUID().toString()
    CollectionEtags.insert {
        it[CollectionEtags.collection] = collection.key
        it[owner] = ownerId
        it[CollectionEtags.version] = version
    }
    return version
}
