package com.dozy.api.client.place

import com.dozy.api.client.DozyClientConfig
import com.dozy.api.client.InstallationIdProvider
import com.dozy.api.client.place.v1.KtorPlacesClient
import com.dozy.api.client.place.v1.PlacesClient as PlacesClientV1
import io.ktor.client.HttpClient

class PlacesClient internal constructor(
    http: HttpClient,
    config: DozyClientConfig,
    installations: InstallationIdProvider,
) {
    val v1: PlacesClientV1 = KtorPlacesClient(http, config, installations)
}
