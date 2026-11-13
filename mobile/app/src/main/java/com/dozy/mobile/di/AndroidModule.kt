package com.dozy.mobile.di

import androidx.work.WorkManager
import com.dozy.mobile.core.config.ApiConfig
import com.dozy.mobile.core.data.api.HttpEngineFactory
import com.dozy.mobile.core.data.api.httpClientModule
import com.dozy.mobile.core.data.database.DatabaseBuilderFactory
import com.dozy.mobile.core.data.database.androidDatabaseBuilder
import com.dozy.mobile.core.data.database.databaseModule
import com.dozy.mobile.feature.character.domain.model.Character
import com.dozy.mobile.feature.character.domain.usecase.CharacterIdGenerator
import com.dozy.mobile.feature.place.di.BundledPlaces
import com.dozy.mobile.feature.place.domain.model.Place
import com.dozy.mobile.feature.place.domain.usecase.PlaceIdGenerator
import com.dozy.mobile.feature.story.data.background.*
import com.dozy.mobile.feature.story.domain.usecase.StoryCompletionNotifier
import com.dozy.mobile.resources.*
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
        // Enable after the backend exposes the agreed API contract.
        single { ApiConfig(baseUrl = null) }
        includes(databaseModule, httpClientModule, imageModule)
        single<DatabaseBuilderFactory> { DatabaseBuilderFactory { androidDatabaseBuilder(androidContext()) } }
        single<HttpEngineFactory> { HttpEngineFactory { Android.create() } }
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
            kotlinx.coroutines.runBlocking {
                suspend fun place(
                    id: String,
                    name: org.jetbrains.compose.resources.StringResource,
                    description: org.jetbrains.compose.resources.StringResource,
                    inhabitants: org.jetbrains.compose.resources.StringResource?,
                ) = Place(
                    Place.Id(id),
                    Place.Name(
                        org.jetbrains.compose.resources
                            .getString(name),
                    ),
                    Place.Description(
                        org.jetbrains.compose.resources
                            .getString(description),
                    ),
                    inhabitants?.let {
                        Place.Inhabitants(
                            org.jetbrains.compose.resources
                                .getString(it),
                        )
                    },
                    Place.Ownership.PREDEFINED,
                    false,
                )
                BundledPlaces(
                    listOf(
                        place(
                            "builtin:woods",
                            Res.string.place_woods,
                            Res.string.place_woods_description,
                            Res.string.place_woods_inhabitants,
                        ),
                        place("builtin:island", Res.string.place_island, Res.string.place_island_description, null),
                        place(
                            "builtin:village",
                            Res.string.place_village,
                            Res.string.place_village_description,
                            Res.string.place_village_inhabitants,
                        ),
                    ),
                )
            }
        }
        single<CharacterIdGenerator> { CharacterIdGenerator { Character.Id(UUID.randomUUID().toString()) } }
    }
