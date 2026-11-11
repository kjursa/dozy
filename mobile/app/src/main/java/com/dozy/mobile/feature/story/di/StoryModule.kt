package com.dozy.mobile.feature.story.di

import androidx.lifecycle.SavedStateHandle
import com.dozy.mobile.feature.story.data.DefaultGenerationRepository
import com.dozy.mobile.feature.story.data.api.GenerationApi
import com.dozy.mobile.feature.story.data.api.KtorGenerationApi
import com.dozy.mobile.feature.story.domain.repository.GenerationRepository
import com.dozy.mobile.feature.story.domain.usecase.StartGeneration
import com.dozy.mobile.feature.story.presentation.create.CreateStoryUiState
import com.dozy.mobile.feature.story.presentation.create.CreateStoryViewModel
import com.dozy.mobile.feature.story.presentation.list.StoriesViewModel
import com.kjursa.foundation.compose.mvi.SavedStatePersistence
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

internal val storyModule =
    module {
        single<GenerationApi> { KtorGenerationApi(get(), get()) }
        single<GenerationRepository> { DefaultGenerationRepository(get(), get()) }
        factoryOf(::StartGeneration)
        viewModelOf(::StoriesViewModel)
        viewModel {
            val handle: SavedStateHandle = get()
            CreateStoryViewModel(
                SavedStatePersistence(handle, CreateStoryUiState.serializer()),
                handle.get<String>("place"),
                get(),
                get(),
                get(),
                get(),
                handle
                    .get<String>("cast")
                    ?.let {
                        kotlinx.serialization.json.Json
                            .decodeFromString<List<String>>(it)
                    }.orEmpty(),
            )
        }
    }
