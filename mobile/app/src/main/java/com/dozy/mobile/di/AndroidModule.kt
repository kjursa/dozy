package com.dozy.mobile.di

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.dozy.mobile.data.CharacterDao
import com.dozy.mobile.data.CharacterDatabase
import com.dozy.mobile.domain.CharacterId
import com.dozy.mobile.domain.CharacterIdGenerator
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import org.koin.dsl.onClose
import java.util.UUID

/** Android construction boundary; a future iOS module supplies equivalent dependencies. */
internal val androidModule =
    module {
        single {
            Room
                .databaseBuilder<CharacterDatabase>(androidContext(), "characters.db")
                .setDriver(BundledSQLiteDriver())
                .build()
        } onClose { it?.close() }
        single<CharacterDao> { get<CharacterDatabase>().characters() }
        single {
            HttpClient(Android) {
                install(ContentNegotiation) { json() }
                install(HttpTimeout) { requestTimeoutMillis = 5000 }
            }
        } onClose { it?.close() }
        single<CharacterIdGenerator> { CharacterIdGenerator { CharacterId(UUID.randomUUID().toString()) } }
    }
