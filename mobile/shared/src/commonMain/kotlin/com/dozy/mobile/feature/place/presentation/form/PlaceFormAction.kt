package com.dozy.mobile.feature.place.presentation.form

sealed interface PlaceFormAction {
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
