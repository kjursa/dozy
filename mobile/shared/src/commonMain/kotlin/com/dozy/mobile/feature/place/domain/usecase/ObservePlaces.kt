package com.dozy.mobile.feature.place.domain.usecase

import com.dozy.mobile.feature.place.domain.model.Place
import com.dozy.mobile.feature.place.domain.repository.PlaceRepository
import kotlinx.coroutines.flow.map

class ObservePlaces(
    private val repository: PlaceRepository,
) {
    operator fun invoke() =
        repository.observe().map { places ->
            places.sortedWith(
                compareBy<Place> { it.origin != Place.Origin.USER }.thenBy { it.name.value.lowercase() }.thenBy { it.id.value },
            )
        }
}
