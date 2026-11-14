package com.dozy.mobile.core.data.api

import com.dozy.api.client.DozyClient
import com.dozy.api.client.DozyClientConfig
import com.dozy.mobile.core.config.ApiConfig
import org.koin.dsl.module

val dozyClientModule = module {
    single {
        DozyClient(get(), DozyClientConfig(get<ApiConfig>().baseUrl), get())
    }
}
