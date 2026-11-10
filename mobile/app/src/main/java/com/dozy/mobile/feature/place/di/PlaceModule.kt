package com.dozy.mobile.feature.place.di

import androidx.lifecycle.SavedStateHandle
import com.dozy.mobile.feature.place.data.DefaultPlaceRepository
import com.dozy.mobile.feature.place.data.api.KtorPlaceApi
import com.dozy.mobile.feature.place.data.api.PlaceApi
import com.dozy.mobile.feature.place.domain.model.Place
import com.dozy.mobile.feature.place.domain.repository.PlaceRepository
import com.dozy.mobile.feature.place.domain.usecase.ObservePlaces
import com.dozy.mobile.feature.place.domain.usecase.RefreshPlaces
import com.dozy.mobile.feature.place.domain.usecase.SavePlace
import com.dozy.mobile.feature.place.domain.usecase.SelectPlace
import com.dozy.mobile.feature.place.presentation.form.PlaceFormUiState
import com.dozy.mobile.feature.place.presentation.form.PlaceFormViewModel
import com.dozy.mobile.feature.place.presentation.list.PlacesViewModel
import com.kjursa.foundation.compose.mvi.SavedStatePersistence
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/** The platform supplies the DAO, HTTP client, IDs, and localized bundled catalogue. */
class BundledPlaces(
    val places: List<Place>,
)

internal val placeModule =
    module {
        single<PlaceApi> { KtorPlaceApi(get(), get()) }
        single<PlaceRepository> { DefaultPlaceRepository(get(), get(), get<BundledPlaces>().places) }
        factoryOf(::ObservePlaces)
        factoryOf(::RefreshPlaces)
        factoryOf(::SelectPlace)
        factoryOf(::SavePlace)
        viewModelOf(::PlacesViewModel)
        viewModel {
            val handle: SavedStateHandle = get()
            PlaceFormViewModel(SavedStatePersistence(handle, PlaceFormUiState.serializer()), get(), get(), handle.get<String>("id"))
        }
    }
