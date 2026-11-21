package com.dozy.mobile.core.data.api

import com.dozy.api.client.DozyClient
import com.dozy.api.client.DozyClientConfig
import com.dozy.api.client.auth.AuthSessionStorage
import com.dozy.mobile.core.config.ApiConfig
import org.koin.dsl.module
import org.koin.dsl.onClose

val dozyClientModule =
    module {
        single { get<DozyClient>().storyGenerations }
        single { get<DozyClient>().stories }
        single { get<DozyClient>().sessionManager }
        single {
            DozyClient(get(), DozyClientConfig(get<ApiConfig>().baseUrl), get<AuthSessionStorage>())
        } onClose { it?.close() }
    }
