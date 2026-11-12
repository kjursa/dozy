package com.dozy.mobile.feature.story.di

import androidx.lifecycle.SavedStateHandle
import com.dozy.mobile.feature.story.presentation.create.CreateStoryUiState
import com.dozy.mobile.feature.story.presentation.create.CreateStoryViewModel
import com.dozy.mobile.feature.story.presentation.generation.GenerationViewModel
import com.dozy.mobile.feature.story.presentation.list.StoriesViewModel
import com.dozy.mobile.feature.story.presentation.reader.SavedStoryViewModel
import com.kjursa.foundation.compose.mvi.SavedStatePersistence
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

internal val storyModule =
    module {
        includes(storyDataModule)
        viewModelOf(::GenerationViewModel)
        viewModelOf(::SavedStoryViewModel)
        viewModel {
            val handle: SavedStateHandle = get()
            com.dozy.mobile.feature.story.presentation.reader.ReaderViewModel(
                com.dozy.mobile.feature.story.domain.model.Story
                    .Id(requireNotNull(handle.get<String>("storyId"))),
                get(),
                SavedStatePersistence(
                    handle,
                    com.dozy.mobile.feature.story.presentation.reader.ReaderUiState
                        .serializer(),
                ),
            )
        }
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
