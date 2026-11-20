package com.dozy.features.place.data.database

import com.dozy.domain.id.PlaceId
import com.dozy.features.place.domain.model.Place
import org.jetbrains.exposed.v1.core.ResultRow

internal fun ResultRow.toPlace() =
    Place(
        id = requireNotNull(PlaceId.parse(this[Places.id])),
        name = Place.Name(this[Places.name]),
        description = Place.Description(this[Places.description]),
        inhabitants = this[Places.inhabitants]?.let { Place.Inhabitants(it) },
        origin = this[Places.origin],
        thumbnail = this[Places.thumbnail],
    )
