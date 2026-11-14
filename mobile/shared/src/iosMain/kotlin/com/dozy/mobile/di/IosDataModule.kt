package com.dozy.mobile.di

import com.dozy.api.client.InstallationIdProvider
import com.dozy.mobile.core.config.ApiConfig
import com.dozy.mobile.core.data.api.HttpEngineFactory
import com.dozy.mobile.core.data.api.dozyClientModule
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
import platform.Foundation.NSUUID
import platform.Foundation.NSUserDefaults

/** The host supplies configuration and its localized catalog; no notification delivery is registered. */
fun iosDataModule(
    config: ApiConfig,
    places: BundledPlaces,
) = module {
    includes(databaseModule, httpClientModule, dozyClientModule, characterDataModule, placeDataModule, storyDataModule)
    single { config }
    single<InstallationIdProvider> {
        val defaults = NSUserDefaults.standardUserDefaults
        val id = defaults.stringForKey("dozy-installation-id") ?: NSUUID().UUIDString.lowercase().also {
            defaults.setObject(it, "dozy-installation-id")
        }
        InstallationIdProvider { id }
    }
    single { places }
    single<DatabaseBuilderFactory> { DatabaseBuilderFactory { iosDatabaseBuilder() } }
    single<HttpEngineFactory> { HttpEngineFactory { Darwin.create() } }
}
