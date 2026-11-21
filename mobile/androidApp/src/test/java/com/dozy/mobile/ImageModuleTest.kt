package com.dozy.mobile

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import com.dozy.mobile.di.androidModule
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class ImageModuleTest {
    @Test fun imageLoaderReusesAndroidContextAndInjectedClientAndReturnsNetworkErrors() =
        runBlocking {
            val context = RuntimeEnvironment.getApplication()
            val requests = mutableListOf<String>()
            val client =
                HttpClient(
                    MockEngine { request ->
                        requests += request.url.toString()
                        respond("missing", HttpStatusCode.NotFound)
                    },
                )
            val graph =
                koinApplication {
                    androidContext(context)
                    modules(androidModule, module { single<HttpClient> { client } })
                }
            try {
                assertSame(context, graph.koin.get<PlatformContext>())
                val loader = graph.koin.get<ImageLoader>()
                try {
                    assertSame(loader, graph.koin.get<ImageLoader>())
                    val result =
                        loader.execute(
                            ImageRequest.Builder(context).data("https://example.com/missing-cover.png").build(),
                        )
                    assertTrue(result is ErrorResult)
                    assertEquals(listOf("https://example.com/missing-cover.png"), requests)
                } finally {
                    loader.shutdown()
                }
            } finally {
                graph.close()
                client.close()
            }
        }
}
