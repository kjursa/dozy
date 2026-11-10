package com.dozy.mobile.feature.character.presentation.create

internal sealed interface CreateCharacterAction {
    data class InitializeSamples(
        val name: String,
        val personality: String,
        val appearance: String,
    ) : CreateCharacterAction

    data object BackClicked : CreateCharacterAction

    data object SaveClicked : CreateCharacterAction

    data object PreviewClicked : CreateCharacterAction

    data class TypeSelected(
        val type: String,
    ) : CreateCharacterAction

    data class NameChanged(
        val name: String,
    ) : CreateCharacterAction

    data class PersonalityChanged(
        val personality: String,
    ) : CreateCharacterAction

    data class AppearanceChanged(
        val appearance: String,
    ) : CreateCharacterAction
}
