package com.dozy.mobile.di

import androidx.work.WorkManager
import com.dozy.api.client.InstallationIdProvider
import com.dozy.mobile.core.config.ApiConfig
import com.dozy.mobile.core.data.api.HttpEngineFactory
import com.dozy.mobile.core.data.api.dozyClientModule
import com.dozy.mobile.core.data.api.httpClientModule
import com.dozy.mobile.core.data.database.DatabaseBuilderFactory
import com.dozy.mobile.core.data.database.androidDatabaseBuilder
import com.dozy.mobile.core.data.database.databaseModule
import com.dozy.mobile.feature.character.data.UuidCharacterIdGenerator
import com.dozy.mobile.feature.character.domain.usecase.CharacterIdGenerator
import com.dozy.mobile.feature.place.di.BundledPlaces
import com.dozy.mobile.feature.place.domain.model.Place
import com.dozy.mobile.feature.place.domain.usecase.PlaceIdGenerator
import com.dozy.mobile.feature.story.data.background.*
import com.dozy.mobile.feature.story.domain.model.GenerationJob
import com.dozy.mobile.feature.story.domain.usecase.GenerationKeyGenerator
import com.dozy.mobile.feature.story.domain.usecase.StoryCompletionNotifier
import com.dozy.mobile.resources.*
import io.ktor.client.engine.android.Android
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
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
        includes(databaseModule, httpClientModule, dozyClientModule, imageModule)
        single<InstallationIdProvider> { AndroidInstallationIdProvider(androidContext()) }
        single<DatabaseBuilderFactory> { DatabaseBuilderFactory { androidDatabaseBuilder(androidContext()) } }
        single<HttpEngineFactory> { HttpEngineFactory { Android.create() } }
        single<GenerationKeyGenerator> {
            GenerationKeyGenerator {
                GenerationJob
                    .RequestKey(UUID.randomUUID().toString())
            }
        }
        single<PlaceIdGenerator> {
            PlaceIdGenerator { Place.Id(UUID.randomUUID().toString()) }
        }
        single {
            runBlocking {
                suspend fun place(
                    id: String,
                    name: StringResource,
                    description: StringResource,
                    inhabitants: StringResource?,
                ) = Place(
                    Place.Id(id),
                    Place.Name(
                        getString(name),
                    ),
                    Place.Description(
                        getString(description),
                    ),
                    inhabitants?.let {
                        Place.Inhabitants(
                            getString(it),
                        )
                    },
                    Place.Origin.SYSTEM,
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
        single<CharacterIdGenerator> { UuidCharacterIdGenerator() }
    }
