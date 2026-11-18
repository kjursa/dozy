package com.dozy.mobile.feature.place.di

import androidx.lifecycle.SavedStateHandle
import com.dozy.mobile.feature.place.presentation.form.PlaceFormUiState
import com.dozy.mobile.feature.place.presentation.form.PlaceFormViewModel
import com.dozy.mobile.feature.place.presentation.list.PlacesViewModel
import com.dozy.mobile.navigation.navigationModule
import com.kjursa.foundation.compose.mvi.SavedStatePersistence
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val placeModule =
    module {
        includes(navigationModule)
        includes(placeDataModule)
        viewModelOf(::PlacesViewModel)
        viewModel {
            val handle: SavedStateHandle = get()
            PlaceFormViewModel(get(), SavedStatePersistence(handle, PlaceFormUiState.serializer()), get(), get(), handle.get<String>("id"))
        }
    }
