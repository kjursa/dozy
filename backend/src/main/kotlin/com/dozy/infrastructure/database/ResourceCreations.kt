package com.dozy.infrastructure.database

import com.dozy.domain.id.IdempotencyKey
import com.dozy.domain.id.OwnerId
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import java.security.MessageDigest
import java.util.HexFormat

internal class ResourceCreation(
    private val ownerId: OwnerId,
    private val resourceType: String,
    private val key: IdempotencyKey,
    fields: List<String?>
) {
    private val requestHash = HexFormat.of().formatHex(
        MessageDigest.getInstance(
            "SHA-256"
        ).digest(Json.encodeToString(fields).encodeToByteArray())
    )

    fun previous(): Previous? = ResourceCreations.selectAll().where {
        (ResourceCreations.ownerId eq ownerId.value) and
            (ResourceCreations.type eq resourceType) and
            (ResourceCreations.key eq key.value)
    }.singleOrNull()?.let {
        Previous(it[ResourceCreations.resourceId], it[ResourceCreations.requestHash] == requestHash)
    }

    fun record(id: String) {
        ResourceCreations.insert {
            it[ResourceCreations.ownerId] = this@ResourceCreation.ownerId.value
            it[type] = resourceType
            it[ResourceCreations.key] = this@ResourceCreation.key.value
            it[ResourceCreations.requestHash] = this@ResourceCreation.requestHash
            it[resourceId] = id
        }
    }

    data class Previous(val id: String, val sameInput: Boolean)
}

private object ResourceCreations : Table("resource_creations") {
    val ownerId = reference("owner_id", OwnerPartitions.id)
    val type = text("resource_type")
    val key = text("request_key")
    val requestHash = text("request_hash")
    val resourceId = text("resource_id")
    override val primaryKey = PrimaryKey(ownerId, type, key)
}
