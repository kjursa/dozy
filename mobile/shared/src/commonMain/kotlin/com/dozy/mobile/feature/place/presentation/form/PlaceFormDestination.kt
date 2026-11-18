package com.dozy.mobile.feature.place.presentation.form

import com.dozy.mobile.feature.place.domain.model.Place
import com.dozy.mobile.navigation.NavigationCommand

sealed interface PlaceFormDestination : NavigationCommand {
    data object Back : PlaceFormDestination

    data class Saved(
        val id: Place.Id,
    ) : PlaceFormDestination
}
