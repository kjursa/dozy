package com.dozy.features.place.data.database

import com.dozy.features.place.domain.model.Place
import org.jetbrains.exposed.v1.core.ResultRow

internal fun ResultRow.toPlace() = Place(
    id = Place.Id(this[Places.id]),
    name = this[Places.name],
    description = this[Places.description],
    inhabitants = this[Places.inhabitants],
    ownership = this[Places.ownership],
)
