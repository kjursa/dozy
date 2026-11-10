package com.dozy.mobile.feature.character.presentation.create

internal sealed interface CreateCharacterEffect {
    data object NavigateBack : CreateCharacterEffect

    data object Saved : CreateCharacterEffect

    data object PreviewRequested : CreateCharacterEffect
}
