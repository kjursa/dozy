package com.dozy.features.place.api

import kotlinx.serialization.Serializable

@Serializable
data class SavePlaceRequest(
    val name: String,
    val description: String,
    val inhabitants: String? = null,
)

@Serializable
data class PlaceResponse(
    val id: String,
    val name: String,
    val description: String,
    val origin: String,
    val editable: Boolean,
    val inhabitants: String? = null,
)

@Serializable
data class PlacesResponse(val places: List<PlaceResponse>)
