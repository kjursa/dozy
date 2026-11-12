package com.dozy.mobile.di

import com.dozy.mobile.core.config.ApiConfig
import com.dozy.mobile.core.data.api.HttpEngineFactory
import com.dozy.mobile.core.data.api.httpClientModule
import com.dozy.mobile.core.data.database.DatabaseBuilderFactory
import com.dozy.mobile.core.data.database.databaseModule
import com.dozy.mobile.core.data.database.iosDatabaseBuilder
import com.dozy.mobile.feature.character.di.characterDataModule
import com.dozy.mobile.feature.place.di.BundledPlaces
import com.dozy.mobile.feature.place.di.placeDataModule
import com.dozy.mobile.feature.story.di.storyDataModule
import io.ktor.client.engine.darwin.Darwin
import org.koin.dsl.module

/** The host supplies configuration and its localized catalog; no notification delivery is registered. */
fun iosDataModule(
    config: ApiConfig,
    places: BundledPlaces,
) = module {
    includes(databaseModule, httpClientModule, characterDataModule, placeDataModule, storyDataModule)
    single { config }
    single { places }
    single<DatabaseBuilderFactory> { DatabaseBuilderFactory { iosDatabaseBuilder() } }
    single<HttpEngineFactory> { HttpEngineFactory { Darwin.create() } }
}
