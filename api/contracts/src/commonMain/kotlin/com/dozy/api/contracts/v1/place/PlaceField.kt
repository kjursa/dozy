package com.dozy.api.contracts.v1.place

import com.dozy.api.contracts.v1.ValidationReason

enum class PlaceField(val value: String, val reasons: Set<ValidationReason>) {
    NAME("name", setOf(ValidationReason.REQUIRED, ValidationReason.TOO_LONG)),
    DESCRIPTION("description", setOf(ValidationReason.REQUIRED, ValidationReason.TOO_LONG)),
    INHABITANTS("inhabitants", setOf(ValidationReason.TOO_LONG));

    companion object {
        fun fromValue(value: String): PlaceField? = entries.firstOrNull { it.value == value }
    }
}
