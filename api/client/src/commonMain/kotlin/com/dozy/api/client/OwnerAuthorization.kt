package com.dozy.api.client

import com.dozy.api.client.auth.AuthSessionManager
import com.dozy.api.contracts.ApiVersion
import io.ktor.client.plugins.api.Send
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.request.bearerAuth
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url

internal fun ownerAuthorization(baseUrl: String, sessions: AuthSessionManager) =
    createClientPlugin("DozyOwnerAuthorization") {
        val origin = Url(baseUrl)
        val prefix = origin.encodedPath.trimEnd('/')
        on(Send) { request ->
            val url = request.url.build()
            if (url.protocol != origin.protocol || url.host != origin.host || url.port != origin.port ||
                (!url.encodedPath.startsWith("${prefix}${ApiVersion.V1.path}/") &&
                    !url.encodedPath.startsWith("$prefix/assets/"))
            ) return@on proceed(request)

            request.headers.remove("X-Installation-Id")
            val sent = sessions.currentTokens() ?: return@on proceed(request)
            request.headers.remove(HttpHeaders.Authorization)
            request.bearerAuth(sent.accessToken)
            val call = proceed(request)
            if (call.response.status != HttpStatusCode.Unauthorized) return@on call

            val refreshed = sessions.refreshForRequest(sent) ?: return@on call
            request.headers.remove(HttpHeaders.Authorization)
            request.bearerAuth(refreshed.accessToken)
            proceed(request)
        }
    }
