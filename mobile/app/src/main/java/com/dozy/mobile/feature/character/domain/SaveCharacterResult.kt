package com.dozy.mobile.feature.character.domain

sealed interface SaveCharacterResult {
    data class Success(
        val saved: SavedCharacter,
    ) : SaveCharacterResult

    data object Failure : SaveCharacterResult
}
