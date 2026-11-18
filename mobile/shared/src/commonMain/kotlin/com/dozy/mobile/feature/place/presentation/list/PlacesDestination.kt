package com.dozy.mobile.feature.place.presentation.list

import com.dozy.mobile.feature.place.domain.model.Place
import com.dozy.mobile.navigation.NavigationCommand

sealed interface PlacesDestination : NavigationCommand {
    data object Back : PlacesDestination

    data class Selected(
        val id: Place.Id?,
    ) : PlacesDestination

    data class Form(
        val id: Place.Id?,
    ) : PlacesDestination
}
