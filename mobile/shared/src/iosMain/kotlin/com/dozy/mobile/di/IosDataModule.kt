package com.dozy.mobile.di

import androidx.sqlite.SQLiteDriver
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.dozy.api.client.auth.AuthSessionStorage
import com.dozy.api.client.auth.StoredAuthSession
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
import platform.Foundation.NSUserDefaults

/** The host supplies configuration and its localized catalog; no notification delivery is registered. */
fun iosDataModule(
    config: ApiConfig,
    places: BundledPlaces,
) = module {
    single<SQLiteDriver> { BundledSQLiteDriver() }
    includes(databaseModule, httpClientModule, dozyClientModule, characterDataModule, placeDataModule, storyDataModule)
    single { config }
    single<AuthSessionStorage> { IosAuthSessionStorage() }
    single { places }
    single<DatabaseBuilderFactory> { DatabaseBuilderFactory { iosDatabaseBuilder() } }
    single<HttpEngineFactory> { HttpEngineFactory { Darwin.create() } }
}

private class IosAuthSessionStorage : AuthSessionStorage {
    private val defaults = NSUserDefaults.standardUserDefaults
    override suspend fun load(): StoredAuthSession? {
        val owner = defaults.stringForKey("dozy-auth-owner") ?: return null
        val access = defaults.stringForKey("dozy-auth-access") ?: return null
        val refresh = defaults.stringForKey("dozy-auth-refresh") ?: return null
        return StoredAuthSession(owner, access, refresh, defaults.boolForKey("dozy-auth-guest"))
    }
    override suspend fun save(session: StoredAuthSession) {
        defaults.setObject(session.ownerId, "dozy-auth-owner")
        defaults.setObject(session.accessToken, "dozy-auth-access")
        defaults.setObject(session.refreshToken, "dozy-auth-refresh")
        defaults.setBool(session.anonymous, "dozy-auth-guest")
    }
    override suspend fun clear() {
        defaults.removeObjectForKey("dozy-auth-owner")
        defaults.removeObjectForKey("dozy-auth-access")
        defaults.removeObjectForKey("dozy-auth-refresh")
        defaults.removeObjectForKey("dozy-auth-anonymous")
        defaults.removeObjectForKey("dozy-auth-guest")
    }
}
