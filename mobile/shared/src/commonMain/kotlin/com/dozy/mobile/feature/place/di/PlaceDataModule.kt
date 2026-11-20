package com.dozy.mobile.feature.place.di

import com.dozy.api.client.DozyClient
import com.dozy.api.client.place.PlacesClients
import com.dozy.mobile.feature.place.data.DefaultPlaceRepository
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
        single<PlacesClients> { get<DozyClient>().places }
        single<PlaceRepository> { DefaultPlaceRepository(get(), get(), getOrNull<BundledPlaces>()?.places.orEmpty()) }
        factoryOf(::ObservePlaces)
        factoryOf(::RefreshPlaces)
        factoryOf(::SelectPlace)
        factoryOf(::SavePlace)
    }
