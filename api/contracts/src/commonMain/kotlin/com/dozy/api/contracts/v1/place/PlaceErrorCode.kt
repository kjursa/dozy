package com.dozy.api.contracts.v1.place

enum class PlaceErrorCode(val value: String) {
    NAME_REQUIRED("validation.name.required"),
    NAME_TOO_LONG("validation.name.too_long"),
    DESCRIPTION_REQUIRED("validation.description.required"),
    DESCRIPTION_TOO_LONG("validation.description.too_long"),
    INHABITANTS_TOO_LONG("validation.inhabitants.too_long"),
    FORBIDDEN("place.not_editable"),
    CONFLICT("place.conflict");

    companion object {
        fun fromValue(value: String): PlaceErrorCode? = entries.firstOrNull { it.value == value }
    }
}
