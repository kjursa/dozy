package com.dozy.mobile.feature.place.presentation.form

internal sealed interface PlaceFormAction {
    data class Name(
        val value: String,
    ) : PlaceFormAction

    data class Description(
        val value: String,
    ) : PlaceFormAction

    data class Inhabitants(
        val value: String,
    ) : PlaceFormAction

    data object Save : PlaceFormAction

    data object Back : PlaceFormAction

    data object Retry : PlaceFormAction
}
