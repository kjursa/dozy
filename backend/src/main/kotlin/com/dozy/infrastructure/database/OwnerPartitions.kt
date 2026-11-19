package com.dozy.infrastructure.database

import com.dozy.domain.id.OwnerId
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.insertIgnore

internal object OwnerPartitions : Table("installations") {
    val id = text("id")
    override val primaryKey = PrimaryKey(id)
}

internal fun JdbcTransaction.registerOwnerPartition(ownerId: OwnerId) {
    OwnerPartitions.insertIgnore { it[id] = ownerId.value }
}
