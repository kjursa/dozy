package com.dozy.mobile.feature.place.data.api

import com.dozy.mobile.core.config.ApiConfig
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get

class KtorPlaceApi(
    private val client: HttpClient,
    private val config: ApiConfig,
) : PlaceApi {
    override suspend fun list(): PlacesDto? {
        val base = config.baseUrl ?: return null
        val response = client.get("${base.trimEnd('/')}/places")
        check(response.status.value == 200) { "Places request failed: ${response.status.value}" }
        return response.body()
    }
}
