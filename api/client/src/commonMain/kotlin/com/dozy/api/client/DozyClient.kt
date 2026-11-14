package com.dozy.api.client

import com.dozy.api.client.place.KtorPlacesClient
import com.dozy.api.client.place.PlacesClient
import com.dozy.api.contracts.ApiVersion
import io.ktor.client.HttpClient

class DozyClient(
    httpClient: HttpClient,
    config: DozyClientConfig,
    installationIdProvider: InstallationIdProvider,
) {
    val places: PlacesClient = KtorPlacesClient(httpClient, config, installationIdProvider)
}

data class DozyClientConfig(
    val baseUrl: String?,
    val version: ApiVersion = ApiVersion.V1,
) {
    init {
        require(baseUrl == null || baseUrl.startsWith("http://") || baseUrl.startsWith("https://"))
        require(baseUrl == null || '?' !in baseUrl && '#' !in baseUrl)
    }

    internal fun endpoint(resource: String): String {
        val base = baseUrl ?: throw ApiDisabledException()
        return "${base.trimEnd('/')}${version.path}/$resource"
    }
}

fun interface InstallationIdProvider {
    fun get(): String
}

class ApiDisabledException : IllegalStateException("Dozy API is disabled")
