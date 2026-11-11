package com.dozy.mobile.di

import androidx.room3.Room
import androidx.sqlite.SQLiteDriver
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.work.WorkManager
import com.dozy.mobile.R
import com.dozy.mobile.core.config.ApiConfig
import com.dozy.mobile.core.data.database.DozyDatabase
import com.dozy.mobile.feature.character.data.database.CharacterDao
import com.dozy.mobile.feature.character.domain.model.Character
import com.dozy.mobile.feature.character.domain.usecase.CharacterIdGenerator
import com.dozy.mobile.feature.place.di.BundledPlaces
import com.dozy.mobile.feature.place.domain.model.Place
import com.dozy.mobile.feature.place.domain.usecase.PlaceIdGenerator
import com.dozy.mobile.feature.story.data.background.*
import com.dozy.mobile.feature.story.domain.usecase.StoryCompletionNotifier
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
        single<StoryCompletionNotifier> { AndroidStoryCompletionNotifier(androidContext()) }
        single { GenerationWorkerFactory(get()) }
        single { GenerationWorkScheduler(WorkManager.getInstance(androidContext())) }
        // Enable after the backend exposes the agreed API contract.
        single { ApiConfig(baseUrl = null) }
        single<SQLiteDriver> { BundledSQLiteDriver() }
        single {
            Room
                .databaseBuilder<DozyDatabase>(androidContext(), "dozy.db")
                .setDriver(get())
                .build()
        } onClose { it?.close() }
        single { get<DozyDatabase>().places() }
        single { get<DozyDatabase>().generations() }
        single<com.dozy.mobile.feature.story.domain.usecase.GenerationKeyGenerator> {
            com.dozy.mobile.feature.story.domain.usecase.GenerationKeyGenerator {
                com.dozy.mobile.feature.story.domain.model.GenerationJob
                    .RequestKey(UUID.randomUUID().toString())
            }
        }
        single<PlaceIdGenerator> {
            PlaceIdGenerator { Place.Id(UUID.randomUUID().toString()) }
        }
        single {
            val context = androidContext()

            fun place(
                id: String,
                name: Int,
                description: Int,
                inhabitants: Int?,
            ) = Place(
                Place.Id(id),
                Place.Name(context.getString(name)),
                Place.Description(context.getString(description)),
                inhabitants?.let { Place.Inhabitants(context.getString(it)) },
                Place.Ownership.PREDEFINED,
                false,
            )
            BundledPlaces(
                listOf(
                    place("builtin:woods", R.string.place_woods, R.string.place_woods_description, R.string.place_woods_inhabitants),
                    place("builtin:island", R.string.place_island, R.string.place_island_description, null),
                    place(
                        "builtin:village",
                        R.string.place_village,
                        R.string.place_village_description,
                        R.string.place_village_inhabitants,
                    ),
                ),
            )
        }
        single<CharacterDao> { get<DozyDatabase>().characters() }
        single {
            HttpClient(Android) {
                install(ContentNegotiation) { json() }
                install(HttpTimeout) { requestTimeoutMillis = 5000 }
            }
        } onClose { it?.close() }
        single<CharacterIdGenerator> { CharacterIdGenerator { Character.Id(UUID.randomUUID().toString()) } }
    }
