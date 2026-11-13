package com.dozy.features.place.domain.model

data class Place(
    val id: Id,
    val name: String,
    val description: String,
    val inhabitants: String?,
    val ownership: Ownership,
) {
    @JvmInline value class Id(val value: String)
    enum class Ownership { CUSTOM, PREDEFINED }
}

class PlaceInput private constructor(
    val name: String,
    val description: String,
    val inhabitants: String?,
) {
    companion object {
        fun from(name: String, description: String, inhabitants: String?): PlaceInput? {
            val normalizedName = name.trim()
            val normalizedDescription = description.trim()
            val normalizedInhabitants = inhabitants?.trim()?.takeIf { it.isNotEmpty() }
            if (normalizedName.isEmpty() || normalizedName.length > 80) return null
            if (normalizedDescription.isEmpty() || normalizedDescription.length > 500) return null
            if ((normalizedInhabitants?.length ?: 0) > 500) return null
            return PlaceInput(normalizedName, normalizedDescription, normalizedInhabitants)
        }
    }
}

sealed interface SavePlaceResult {
    data object Created : SavePlaceResult
    data object Updated : SavePlaceResult
    data object Forbidden : SavePlaceResult
    data object Conflict : SavePlaceResult
}
