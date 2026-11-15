package com.dozy.mobile.core.data.api

import com.dozy.api.client.InstallationIdProvider
import com.dozy.mobile.core.config.ApiConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.http.Url
import io.ktor.http.encodedPath
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Job
import org.koin.dsl.module
import org.koin.dsl.onClose

fun interface HttpEngineFactory {
    fun create(): HttpClientEngine
}

fun createHttpClient(
    factory: HttpEngineFactory,
    config: ApiConfig? = null,
    installations: InstallationIdProvider? = null,
): HttpClient {
    val engine = factory.create()
    val client =
        HttpClient(engine) {
            if (config?.baseUrl != null && installations != null) {
                val origin = Url(config.baseUrl)
                install(
                    createClientPlugin("AssetInstallation") {
                        onRequest { request, _ ->
                            if (request.url.protocol == origin.protocol &&
                                request.url.host == origin.host &&
                                request.url.port == origin.port &&
                                request.url.encodedPath.startsWith(origin.encodedPath.trimEnd('/') + "/api/v1/assets/") &&
                                !request.headers.contains("X-Installation-Id")
                            ) {
                                request.header("X-Installation-Id", installations.get())
                            }
                        }
                    },
                )
            }
            install(ContentNegotiation) { json() }
            install(HttpTimeout) { requestTimeoutMillis = 5000 }
        }
    // HttpClient(engine) leaves engine ownership with its caller.
    client.coroutineContext[Job]!!.invokeOnCompletion { engine.close() }
    return client
}

val httpClientModule =
    module {
        single {
            val config = get<ApiConfig>()
            val installation = get<InstallationIdProvider>()
            createHttpClient(get(), config, installation)
        } onClose { it?.close() }
    }
