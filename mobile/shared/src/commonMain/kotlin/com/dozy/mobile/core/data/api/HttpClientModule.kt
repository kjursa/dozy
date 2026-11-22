package com.dozy.mobile.core.data.api

import com.dozy.mobile.core.config.ApiConfig
import com.dozy.mobile.core.logging.DozyLog
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Job
import org.koin.dsl.module
import org.koin.dsl.onClose

fun interface HttpEngineFactory {
    fun create(): HttpClientEngine
}

fun createHttpClient(
    factory: HttpEngineFactory,
    httpLoggingEnabled: Boolean = false,
): HttpClient = createHttpClient(factory, httpLoggingEnabled) { DozyLog.d("DozyHttp") { it } }

internal fun createHttpClient(
    factory: HttpEngineFactory,
    httpLoggingEnabled: Boolean,
    log: (String) -> Unit,
): HttpClient {
    val engine = factory.create()
    val client =
        HttpClient(engine) {
            install(httpDiagnostics(log.takeIf { httpLoggingEnabled }))
            if (httpLoggingEnabled) installHttpResponseBodies(log)
            install(ContentNegotiation) { json() }
            install(HttpTimeout) { requestTimeoutMillis = 5000 }
        }
    // HttpClient(engine) leaves engine ownership with its caller.
    client.coroutineContext[Job]!!.invokeOnCompletion { engine.close() }
    return client
}

val httpClientModule =
    module {
        single { createHttpClient(get(), get<ApiConfig>().httpLoggingEnabled) } onClose { it?.close() }
    }
