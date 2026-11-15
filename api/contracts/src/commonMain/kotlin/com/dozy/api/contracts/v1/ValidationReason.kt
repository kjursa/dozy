package com.dozy.api.contracts.v1

enum class ValidationReason(val value: String) {
    REQUIRED("REQUIRED"),
    TOO_LONG("TOO_LONG"),
    SINGLE_LINE("SINGLE_LINE"),
    INVALID_VALUE("INVALID_VALUE");

    companion object {
        fun fromValue(value: String): ValidationReason? = entries.firstOrNull { it.value == value }
    }
}
