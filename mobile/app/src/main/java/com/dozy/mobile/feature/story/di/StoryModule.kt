package com.dozy.mobile.feature.story.di

import androidx.lifecycle.SavedStateHandle
import com.dozy.mobile.feature.story.presentation.create.CreateStoryUiState
import com.dozy.mobile.feature.story.presentation.create.CreateStoryViewModel
import com.dozy.mobile.feature.story.presentation.list.StoriesViewModel
import com.kjursa.foundation.compose.mvi.SavedStatePersistence
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

internal val storyModule =
    module {
        viewModelOf(::StoriesViewModel)
        viewModel {
            val handle: SavedStateHandle = get()
            CreateStoryViewModel(
                SavedStatePersistence(handle, CreateStoryUiState.serializer()),
                handle.get<String>("place"),
                get(),
            )
        }
    }
