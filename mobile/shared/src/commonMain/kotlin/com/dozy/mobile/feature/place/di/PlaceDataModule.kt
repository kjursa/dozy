package com.dozy.mobile.feature.place.di

import com.dozy.mobile.feature.place.data.DefaultPlaceRepository
import com.dozy.mobile.feature.place.data.api.KtorPlaceApi
import com.dozy.mobile.feature.place.data.api.PlaceApi
import com.dozy.mobile.feature.place.domain.model.Place
import com.dozy.mobile.feature.place.domain.repository.PlaceRepository
import com.dozy.mobile.feature.place.domain.usecase.ObservePlaces
import com.dozy.mobile.feature.place.domain.usecase.RefreshPlaces
import com.dozy.mobile.feature.place.domain.usecase.SavePlace
import com.dozy.mobile.feature.place.domain.usecase.SelectPlace
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

/** The platform supplies the DAO, HTTP client, IDs, and localized bundled catalogue. */
class BundledPlaces(
    val places: List<Place>,
)

val placeDataModule =
    module {
        single<PlaceApi> { KtorPlaceApi(get(), get()) }
        single<PlaceRepository> { DefaultPlaceRepository(get(), get(), get<BundledPlaces>().places) }
        factoryOf(::ObservePlaces)
        factoryOf(::RefreshPlaces)
        factoryOf(::SelectPlace)
        factoryOf(::SavePlace)
    }
