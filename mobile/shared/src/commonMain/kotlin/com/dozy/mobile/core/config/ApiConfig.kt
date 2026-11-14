package com.dozy.mobile.core.config

import com.dozy.api.contracts.ApiVersion

/** A null base URL disables remote requests until the backend API is available. */
data class ApiConfig(
    val baseUrl: String?,
) {
    val apiBaseUrl: String? get() = baseUrl?.trimEnd('/')?.plus(ApiVersion.V1.path)
}
