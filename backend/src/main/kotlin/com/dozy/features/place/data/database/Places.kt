package com.dozy.features.place.data.database

import com.dozy.features.place.domain.model.Place
import com.dozy.infrastructure.database.OwnerPartitions
import org.jetbrains.exposed.v1.core.Table

internal object Places : Table("places") {
    val id = text("id")
    val ownerId = reference("installation_id", OwnerPartitions.id).nullable()
    val name = text("name")
    val description = text("description")
    val inhabitants = text("inhabitants").nullable()
    val origin = enumerationByName<Place.Origin>("origin", 16)
    val thumbnail = enumerationByName<Place.Thumbnail>("thumbnail", 16).default(Place.Thumbnail.COTTAGE)
    val updatedAt = text("updated_at")
    override val primaryKey = PrimaryKey(id)
}
