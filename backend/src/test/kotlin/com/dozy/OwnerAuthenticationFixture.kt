package com.dozy

import com.dozy.config.AuthSecretProvider
import com.dozy.features.auth.application.AccessTokens
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.bearerAuth
import io.ktor.server.config.MapApplicationConfig
import java.time.Clock

internal fun HttpRequestBuilder.asOwner(id: String) {
    bearerAuth(AccessTokens(AuthSecretProvider.from(MapApplicationConfig()), Clock.systemUTC()).issue(id.lowercase()))
}
