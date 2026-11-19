package com.dozy.mobile

import androidx.compose.ui.window.ComposeUIViewController
import com.dozy.mobile.core.config.ApiConfig
import com.dozy.mobile.di.iosDataModule
import com.dozy.mobile.di.iosPresentationModule
import com.dozy.mobile.feature.place.di.BundledPlaces
import com.dozy.mobile.feature.story.domain.usecase.GenerationKeyGenerator
import org.koin.core.context.startKoin
import org.koin.dsl.module

/** Call once before constructing a controller. The embedding host owns request keys and configuration. */
fun initializeDozy(
    config: ApiConfig,
    places: BundledPlaces,
    requestKeys: GenerationKeyGenerator,
) {
    startKoin {
        modules(
            iosDataModule(config, places),
            iosPresentationModule,
            module {
                single { requestKeys }
            },
        )
    }
}

/** Shared navigation and UI; foreground refresh works without background delivery. */
fun DozyViewController() = ComposeUIViewController { DozyApp() }
