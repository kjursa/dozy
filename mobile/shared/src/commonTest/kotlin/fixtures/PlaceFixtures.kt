package com.dozy.mobile

import com.dozy.mobile.feature.place.domain.model.Place
import com.dozy.mobile.feature.place.domain.repository.PlaceRepository
import kotlinx.coroutines.flow.MutableStateFlow

internal fun place(
    id: String = "custom",
    name: String = "Garden",
    custom: Boolean = true,
    editable: Boolean = custom,
) = Place(
    Place.Id(id),
    Place.Name(name),
    Place.Description("A little wooden gate"),
    null,
    if (custom) Place.Origin.USER else Place.Origin.SYSTEM,
    editable,
)

internal class FakePlaceRepository : PlaceRepository {
    val places = MutableStateFlow(listOf(place(), place("builtin:woods", "Woods", false)))
    var findFailure = false
    var refreshCalls = 0
    var saves = 0
    var failure = false
    var refreshBlock: suspend () -> Boolean = { true }

    override fun observe() = places

    override suspend fun refresh(): Boolean {
        refreshCalls++
        return refreshBlock()
    }

    override suspend fun find(id: Place.Id): Place? {
        check(!findFailure)
        return places.value.find { it.id == id }
    }

    override suspend fun save(place: Place): com.dozy.mobile.feature.place.domain.model.PlaceSync {
        saves++
        check(!failure)
        places.value = places.value.filterNot { it.id == place.id } + place
        return com.dozy.mobile.feature.place.domain.model.PlaceSync.PENDING
    }
}
