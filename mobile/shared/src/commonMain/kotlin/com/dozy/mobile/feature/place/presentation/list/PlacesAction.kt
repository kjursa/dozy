package com.dozy.mobile.feature.place.presentation.list

import com.dozy.mobile.feature.place.domain.model.Place

sealed interface PlacesAction {
    data object Retry : PlacesAction

    data object Back : PlacesAction

    data object Add : PlacesAction

    data object Surprise : PlacesAction

    data class Select(
        val id: Place.Id,
    ) : PlacesAction

    data class Edit(
        val id: Place.Id,
    ) : PlacesAction
}
