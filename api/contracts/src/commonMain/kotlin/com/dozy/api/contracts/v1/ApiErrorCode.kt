package com.dozy.api.contracts.v1

enum class ApiErrorCode(val value: String) {
    INVALID_REQUEST("INVALID_REQUEST"),
    VALIDATION_FAILED("VALIDATION_FAILED"),
    INVALID_INSTALLATION_ID("INVALID_INSTALLATION_ID"),
    FORBIDDEN("FORBIDDEN"),
    CONFLICT("CONFLICT"),
    CHARACTER_NOT_FOUND("CHARACTER_NOT_FOUND"),
    REFERENCE_UNAVAILABLE("REFERENCE_UNAVAILABLE"),
    UNAVAILABLE("UNAVAILABLE");

    companion object {
        fun fromValue(value: String): ApiErrorCode? = entries.firstOrNull { it.value == value }
    }
}
