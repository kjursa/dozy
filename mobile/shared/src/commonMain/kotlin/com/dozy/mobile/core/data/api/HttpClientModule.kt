package com.dozy.mobile.core.data.api

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

fun createHttpClient(factory: HttpEngineFactory): HttpClient {
    val engine = factory.create()
    val client =
        HttpClient(engine) {
            install(ContentNegotiation) { json() }
            install(HttpTimeout) { requestTimeoutMillis = 5000 }
        }
    // HttpClient(engine) leaves engine ownership with its caller.
    client.coroutineContext[Job]!!.invokeOnCompletion { engine.close() }
    return client
}

val httpClientModule =
    module {
        single { createHttpClient(get()) } onClose { it?.close() }
    }
