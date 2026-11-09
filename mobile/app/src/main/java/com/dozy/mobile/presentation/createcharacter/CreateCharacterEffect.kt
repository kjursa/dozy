package com.dozy.mobile.presentation.createcharacter

internal sealed interface CreateCharacterEffect {
    data object NavigateBack : CreateCharacterEffect

    data object Saved : CreateCharacterEffect

    data object PreviewRequested : CreateCharacterEffect
}
