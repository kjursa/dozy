package com.dozy.mobile.di

import androidx.work.WorkManager
import com.dozy.api.client.auth.AuthSessionStorage
import com.dozy.mobile.BuildConfig
import com.dozy.mobile.core.config.ApiConfig
import com.dozy.mobile.core.data.api.HttpEngineFactory
import com.dozy.mobile.core.data.api.dozyClientModule
import com.dozy.mobile.core.data.api.httpClientModule
import com.dozy.mobile.core.data.database.DatabaseBuilderFactory
import com.dozy.mobile.core.data.database.androidDatabaseBuilder
import com.dozy.mobile.core.data.database.databaseModule
import com.dozy.mobile.feature.story.data.background.AndroidStoryCompletionNotifier
import com.dozy.mobile.feature.story.data.background.GenerationWorkScheduler
import com.dozy.mobile.feature.story.data.background.GenerationWorkerFactory
import com.dozy.mobile.feature.story.domain.model.GenerationJob
import com.dozy.mobile.feature.story.domain.usecase.GenerationKeyGenerator
import com.dozy.mobile.feature.story.domain.usecase.StoryCompletionNotifier
import io.ktor.client.engine.android.Android
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import java.util.UUID

/** Android construction boundary for shared data and Android background work. */
internal val androidModule =
    module {
        single<StoryCompletionNotifier> { AndroidStoryCompletionNotifier(androidContext()) }
        single { GenerationWorkerFactory(get()) }
        single { GenerationWorkScheduler(WorkManager.getInstance(androidContext())) }
        single { ApiConfig(baseUrl = BuildConfig.DOZY_API_BASE_URL, allowLocalAssetHttp = BuildConfig.ALLOW_LOCAL_ASSET_HTTP) }
        includes(databaseModule, httpClientModule, dozyClientModule, imageModule)
        single<AuthSessionStorage> { AndroidAuthSessionStorage(androidContext()) }
        single<DatabaseBuilderFactory> { DatabaseBuilderFactory { androidDatabaseBuilder(androidContext()) } }
        single<HttpEngineFactory> { HttpEngineFactory { Android.create() } }
        single<GenerationKeyGenerator> {
            GenerationKeyGenerator {
                GenerationJob
                    .RequestKey(UUID.randomUUID().toString())
            }
        }
    }
