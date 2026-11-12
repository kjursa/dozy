package com.dozy.mobile.core.config

/** A null base URL disables remote requests until the backend API is available. */
data class ApiConfig(
    val baseUrl: String?,
)
