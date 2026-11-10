package com.dozy.mobile

import com.dozy.mobile.feature.place.data.database.PlaceDao
import com.dozy.mobile.feature.place.data.database.PlaceEntity
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
    if (custom) Place.Ownership.CUSTOM else Place.Ownership.PREDEFINED,
    editable,
)

internal class FakePlaceDao : PlaceDao() {
    val rows = MutableStateFlow<List<PlaceEntity>>(emptyList())
    var failure = false

    override fun observe() = rows

    override suspend fun find(id: String) = rows.value.find { it.id == id }

    override suspend fun upsert(place: PlaceEntity) {
        check(!failure)
        rows.value = rows.value.filterNot { it.id == place.id } + place
    }

    override suspend fun deleteRemote() {
        rows.value = rows.value.filter { it.local }
    }
}

internal class FakePlaceRepository : PlaceRepository {
    val places = MutableStateFlow(listOf(place(), place("builtin:woods", "Woods", false)))
    var refreshCalls = 0
    var saves = 0
    var failure = false
    var refreshBlock: suspend () -> Boolean = { true }

    override fun observe() = places

    override suspend fun refresh(): Boolean {
        refreshCalls++
        return refreshBlock()
    }

    override suspend fun find(id: Place.Id) = places.value.find { it.id == id }

    override suspend fun save(place: Place) {
        saves++
        check(!failure)
        places.value = places.value.filterNot { it.id == place.id } + place
    }
}
