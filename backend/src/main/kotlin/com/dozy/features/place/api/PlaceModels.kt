package com.dozy.features.place.api

import kotlinx.serialization.Serializable

@Serializable
data class SavePlaceRequest(
    val id: String,
    val name: String,
    val description: String,
    val ownership: String,
    val editable: Boolean,
    val inhabitants: String? = null,
)

@Serializable
data class PlaceResponse(
    val id: String,
    val name: String,
    val description: String,
    val ownership: String,
    val editable: Boolean,
    val inhabitants: String? = null,
)

@Serializable
data class PlacesResponse(val places: List<PlaceResponse>)
