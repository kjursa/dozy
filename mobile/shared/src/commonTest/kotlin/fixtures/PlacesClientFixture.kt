package com.dozy.mobile

import com.dozy.api.client.DozyClient
import com.dozy.api.client.DozyClientConfig
import com.dozy.api.client.InstallationIdProvider
import com.dozy.mobile.core.config.ApiConfig
import io.ktor.client.HttpClient

internal fun placesClient(
    http: HttpClient,
    config: ApiConfig,
) = DozyClient(
    http,
    DozyClientConfig(config.baseUrl),
    InstallationIdProvider { "60c55c02-8d8b-46c2-a0cb-9a2d241eeea0" },
).places
