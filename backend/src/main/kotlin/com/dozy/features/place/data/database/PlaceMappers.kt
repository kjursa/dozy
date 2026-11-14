package com.dozy.features.place.data.database

import com.dozy.features.place.domain.model.Place
import org.jetbrains.exposed.v1.core.ResultRow

internal fun ResultRow.toPlace() = Place(
    id = Place.Id.fromDatabase(this[Places.id]),
    name = Place.Name.fromDatabase(this[Places.name]),
    description = Place.Description.fromDatabase(this[Places.description]),
    inhabitants = this[Places.inhabitants]?.let(Place.Inhabitants::fromDatabase),
    ownership = this[Places.ownership],
)
