package com.dozy.mobile.feature.place.data.api

import com.dozy.mobile.core.config.ApiConfig
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.appendPathSegments
import io.ktor.http.contentType

class KtorPlaceApi(
    private val client: HttpClient,
    private val config: ApiConfig,
) : PlaceApi {
    override suspend fun save(place: PlaceDto): Boolean {
        val base = config.baseUrl ?: return false
        val response =
            client.put("${base.trimEnd('/')}/places") {
                url { appendPathSegments(place.id, encodeSlash = true) }
                contentType(ContentType.Application.Json)
                setBody(place)
            }
        return response.status.value in 200..299
    }

    override suspend fun list(): PlacesDto? {
        val base = config.baseUrl ?: return null
        val response = client.get("${base.trimEnd('/')}/places")
        check(response.status.value == 200) { "Places request failed: ${response.status.value}" }
        return response.body()
    }
}
