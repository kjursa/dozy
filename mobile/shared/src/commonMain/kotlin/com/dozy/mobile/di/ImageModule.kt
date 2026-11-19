package com.dozy.mobile.di

import coil3.ImageLoader
import coil3.PlatformContext
import coil3.network.ktor3.KtorNetworkFetcherFactory
import com.dozy.api.client.DozyClient
import org.koin.dsl.module

/** Use the platform engine and the same injected client as shared API calls. */
val imageModule =
    module {
        single {
            ImageLoader
                .Builder(get<PlatformContext>())
                .components { add(KtorNetworkFetcherFactory(httpClient = { get<DozyClient>().assetHttpClient })) }
                .build()
        }
    }
