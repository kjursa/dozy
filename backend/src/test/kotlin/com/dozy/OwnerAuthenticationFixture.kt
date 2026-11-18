package com.dozy

import com.dozy.features.auth.application.AccessTokens
import com.dozy.features.auth.application.developmentAuthSecret
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.bearerAuth

internal fun HttpRequestBuilder.asOwner(id: String) {
    bearerAuth(AccessTokens(developmentAuthSecret).issue(id.lowercase()))
}
