package com.dozy.mobile.feature.story.presentation.reader

sealed interface ReaderAction {
    data object Retry : ReaderAction

    data object Settings : ReaderAction

    data object Cover : ReaderAction

    data object Dismiss : ReaderAction

    data object NewAdventure : ReaderAction

    data object Back : ReaderAction

    data object Library : ReaderAction

    data class Size(
        val size: ReaderUiState.TextSize,
    ) : ReaderAction

    data class Evening(
        val enabled: Boolean,
    ) : ReaderAction
}
