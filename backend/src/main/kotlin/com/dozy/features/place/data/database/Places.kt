package com.dozy.features.place.data.database

import com.dozy.features.installation.data.database.Installations
import com.dozy.features.place.domain.model.Place
import org.jetbrains.exposed.v1.core.Table

internal object Places : Table("places") {
    val id = text("id")
    val installationId = reference("installation_id", Installations.id).nullable()
    val name = text("name")
    val description = text("description")
    val inhabitants = text("inhabitants").nullable()
    val ownership = enumerationByName<Place.Ownership>("ownership", 16)
    val updatedAt = text("updated_at")
    override val primaryKey = PrimaryKey(id)
}
