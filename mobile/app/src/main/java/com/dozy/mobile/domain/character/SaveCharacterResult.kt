package com.dozy.mobile.domain.character

sealed interface SaveCharacterResult {
    data class Success(
        val saved: SavedCharacter,
    ) : SaveCharacterResult

    data object Failure : SaveCharacterResult
}
