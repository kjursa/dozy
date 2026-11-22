package com.dozy.mobile.core.data.api

import com.dozy.api.client.DozyClient
import com.dozy.api.client.DozyClientConfig
import com.dozy.api.client.auth.AuthSessionStorage
import com.dozy.mobile.core.config.ApiConfig
import com.dozy.mobile.core.logging.DozyLog
import org.koin.dsl.module
import org.koin.dsl.onClose

val dozyClientModule =
    module {
        single { get<DozyClient>().storyGenerations }
        single { get<DozyClient>().stories }
        single { get<DozyClient>().sessionManager }
        single {
            val config = get<ApiConfig>()
            val diagnostics: ((String) -> Unit)? =
                if (config.httpLoggingEnabled) ({ message -> DozyLog.d("DozyHttp") { "auth $message" } }) else null
            DozyClient(get(), DozyClientConfig(config.baseUrl, authDiagnostics = diagnostics), get<AuthSessionStorage>())
        } onClose { it?.close() }
    }
