package com.dozy.api.contracts.v1

import kotlinx.serialization.Serializable

@Serializable
data class ApiErrorResponse(
    val code: String,
    val field: String? = null,
    val reason: String? = null,
)

