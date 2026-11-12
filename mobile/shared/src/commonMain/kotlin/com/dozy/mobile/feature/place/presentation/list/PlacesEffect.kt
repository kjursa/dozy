package com.dozy.mobile.feature.place.presentation.list

import com.dozy.mobile.feature.place.domain.model.Place

sealed interface PlacesEffect {
    data object Back : PlacesEffect

    data class Selected(
        val id: Place.Id?,
    ) : PlacesEffect

    data class Form(
        val id: Place.Id?,
    ) : PlacesEffect
}
