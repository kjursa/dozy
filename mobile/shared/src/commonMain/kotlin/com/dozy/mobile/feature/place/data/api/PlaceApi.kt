package com.dozy.mobile.feature.place.data.api

import kotlinx.serialization.Serializable

fun interface PlaceApi {
    /** Null means disabled, never a successful remote refresh. */
    suspend fun list(): PlacesDto?

    /** False means disabled or unacknowledged; the durable row remains pending. */
    suspend fun save(place: PlaceDto): Boolean = false
}

@Serializable
data class PlacesDto(
    val places: List<PlaceDto>,
)

@Serializable
data class PlaceDto(
    val id: String,
    val name: String,
    val description: String,
    val ownership: String,
    val editable: Boolean,
    val inhabitants: String? = null,
)
