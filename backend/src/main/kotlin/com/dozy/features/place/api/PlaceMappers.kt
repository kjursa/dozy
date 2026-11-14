package com.dozy.features.place.api

import com.dozy.features.place.domain.model.Place

fun Place.toResponse() = PlaceResponse(
    id.value, name.value, description.value, origin.name, origin == Place.Origin.USER, inhabitants?.value,
)
