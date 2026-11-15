package com.dozy.mobile.core.config

import com.dozy.api.contracts.ApiVersion
import io.ktor.http.URLProtocol
import io.ktor.http.Url

/** A null base URL disables remote requests until the backend API is available. */
data class ApiConfig(
    val baseUrl: String?,
    val allowLocalAssetHttp: Boolean = false,
) {
    val apiBaseUrl: String? get() = baseUrl?.trimEnd('/')?.plus(ApiVersion.V1.path)
}

/** HTTP is restricted to explicitly enabled development loopback/emulator assets. */
fun isAllowedAssetUrl(
    url: String,
    allowLocalHttp: Boolean = false,
): Boolean =
    runCatching {
        val parsed = Url(url)
        parsed.host.isNotBlank() &&
            parsed.user.isNullOrEmpty() &&
            parsed.password.isNullOrEmpty() &&
            (
                parsed.protocol == URLProtocol.HTTPS ||
                    (
                        allowLocalHttp &&
                            parsed.protocol == URLProtocol.HTTP &&
                            parsed.host in setOf("localhost", "127.0.0.1", "::1", "[::1]", "10.0.2.2")
                    )
            )
    }.getOrDefault(false)
