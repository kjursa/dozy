package com.dozy.mobile.feature.place.presentation.form

import com.dozy.mobile.feature.place.domain.model.Place

sealed interface PlaceFormEffect {
    data object Back : PlaceFormEffect

    data class Saved(
        val id: Place.Id,
    ) : PlaceFormEffect
}
