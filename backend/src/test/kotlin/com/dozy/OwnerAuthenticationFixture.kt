package com.dozy

import com.dozy.features.auth.application.AccessTokens
import com.dozy.config.AuthSecretProvider
import io.ktor.server.config.MapApplicationConfig
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.bearerAuth

internal fun HttpRequestBuilder.asOwner(id: String) {
    bearerAuth(AccessTokens(AuthSecretProvider.from(MapApplicationConfig())).issue(id.lowercase()))
}
