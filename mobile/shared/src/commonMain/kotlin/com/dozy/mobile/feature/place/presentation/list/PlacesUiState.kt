package com.dozy.mobile.feature.place.presentation.list

import com.dozy.mobile.feature.place.domain.model.Place
import com.kjursa.foundation.presentation.text.UiText

data class PlacesUiState(
    val places: List<Place> = emptyList(),
    val loading: Boolean = true,
    val error: UiText? = null,
    val navigating: Boolean = false,
) {
    val hasCustom: Boolean get() = places.any { it.ownership == Place.Ownership.CUSTOM }
}
