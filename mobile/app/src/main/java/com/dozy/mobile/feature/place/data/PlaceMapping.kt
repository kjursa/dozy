package com.dozy.mobile.feature.place.data

import com.dozy.mobile.feature.place.data.api.PlaceDto
import com.dozy.mobile.feature.place.data.database.PlaceEntity
import com.dozy.mobile.feature.place.domain.model.Place

fun PlaceDto.toEntity(): PlaceEntity {
    require(id.isNotBlank() && name.isNotBlank() && description.isNotBlank())
    val owner = Place.Ownership.valueOf(ownership)
    require(!id.startsWith("builtin:") || owner == Place.Ownership.PREDEFINED)
    require(owner != Place.Ownership.PREDEFINED || !editable)
    return PlaceEntity(id, name, description, inhabitants?.takeIf { it.isNotBlank() }, owner.name, editable, false)
}

fun PlaceEntity.toDomain() =
    Place(
        Place.Id(id),
        Place.Name(name),
        Place.Description(description),
        inhabitants?.let(Place::Inhabitants),
        Place.Ownership.valueOf(ownership),
        editable,
    )

fun Place.toEntity() = PlaceEntity(id.value, name.value, description.value, inhabitants?.value, ownership.name, editable, true)

fun Place.toDto() = PlaceDto(id.value, name.value, description.value, ownership.name, editable, inhabitants?.value)
