package com.dozy.api.contracts.v1

enum class ValidationReason(val value: String) {
    REQUIRED("REQUIRED"),
    TOO_LONG("TOO_LONG");

    companion object {
        fun fromValue(value: String): ValidationReason? = entries.firstOrNull { it.value == value }
    }
}
