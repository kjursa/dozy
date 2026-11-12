package com.dozy.mobile.feature.character.presentation.details

sealed interface CharacterDetailsAction {
    data object Back : CharacterDetailsAction

    data object Edit : CharacterDetailsAction

    data object Story : CharacterDetailsAction

    data object Retry : CharacterDetailsAction

    data object Delete : CharacterDetailsAction

    data object ConfirmDelete : CharacterDetailsAction

    data object CancelDelete : CharacterDetailsAction
}
