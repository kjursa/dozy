package com.dozy.mobile.di

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.dozy.mobile.feature.character.data.database.CharacterDao
import com.dozy.mobile.feature.character.data.database.CharacterDatabase
import com.dozy.mobile.feature.character.domain.Character
import com.dozy.mobile.feature.character.domain.CharacterIdGenerator
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
        single<CharacterIdGenerator> { CharacterIdGenerator { Character.Id(UUID.randomUUID().toString()) } }
    }
