package com.dozy.mobile

import com.dozy.mobile.core.config.ApiConfig
import com.dozy.mobile.core.data.api.HttpEngineFactory
import com.dozy.mobile.core.data.api.httpClientModule
import com.dozy.mobile.feature.place.data.api.KtorPlaceApi
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.HttpTimeoutCapability
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.Job
import kotlinx.coroutines.test.runTest
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class HttpClientModuleTest {
    @Test fun graphReusesConfiguredClientAndClosesItsEngine() =
        runTest {
            var engines = 0
            val engine =
                MockEngine {
                    assertEquals(5000L, it.getCapabilityOrNull(HttpTimeoutCapability)?.requestTimeoutMillis)
                    respond("""{"places":[]}""", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
                }
            val graph =
                koinApplication {
                    modules(
                        httpClientModule,
                        module {
                            single<HttpEngineFactory> {
                                HttpEngineFactory {
                                    engines++
                                    engine
                                }
                            }
                        },
                    )
                }
            val client = graph.koin.get<HttpClient>()
            try {
                assertSame(client, graph.koin.get<HttpClient>())
                assertEquals(emptyList(), KtorPlaceApi(client, ApiConfig("https://example.test")).list()?.places)
                assertEquals(emptyList(), KtorPlaceApi(client, ApiConfig("https://example.test")).list()?.places)
                assertEquals(1, engines)
            } finally {
                graph.close()
            }
            client.coroutineContext[Job]!!.join()
            engine.coroutineContext[Job]!!.join()
            assertTrue(engine.coroutineContext[Job]!!.isCompleted)
        }
}
