package com.dozy.mobile.feature.place.data

import com.dozy.mobile.feature.place.data.api.PlaceDto
import com.dozy.mobile.feature.place.data.api.SavePlaceRequestDto
import com.dozy.mobile.feature.place.data.database.PlaceEntity
import com.dozy.mobile.feature.place.domain.model.Place

fun PlaceDto.toEntity(): PlaceEntity {
    require(id.isNotBlank() && name.isNotBlank() && description.isNotBlank())
    val source = Place.Origin.valueOf(origin)
    require(!id.startsWith("builtin:") || source == Place.Origin.SYSTEM)
    require(source != Place.Origin.SYSTEM || !editable)
    return PlaceEntity(id, name, description, inhabitants?.takeIf { it.isNotBlank() }, source.name, editable, false)
}

fun PlaceEntity.toDomain() =
    Place(
        Place.Id(id),
        Place.Name(name),
        Place.Description(description),
        inhabitants?.let(Place::Inhabitants),
        Place.Origin.valueOf(origin),
        editable,
    )

fun Place.toEntity() = PlaceEntity(id.value, name.value, description.value, inhabitants?.value, origin.name, editable, true)

fun Place.toDto() = PlaceDto(id.value, name.value, description.value, origin.name, editable, inhabitants?.value)

fun Place.toSaveRequest() = SavePlaceRequestDto(name.value, description.value, inhabitants?.value)
