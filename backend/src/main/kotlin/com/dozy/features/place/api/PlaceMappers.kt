package com.dozy.features.place.api

import com.dozy.features.place.domain.model.Place

fun Place.toResponse() = PlaceResponse(
    id.value, name, description, ownership.name, ownership == Place.Ownership.CUSTOM, inhabitants,
)
