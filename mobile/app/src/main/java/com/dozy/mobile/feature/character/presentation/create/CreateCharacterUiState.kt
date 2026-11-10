package com.dozy.mobile.feature.character.presentation.create

import com.kjursa.foundation.presentation.text.UiText

internal data class CreateCharacterUiState(
    val nameError: UiText? = null,
    val personalityError: UiText? = null,
    val appearanceError: UiText? = null,
    val typeError: UiText? = null,
    val saveStatus: SaveStatus = SaveStatus.EDITING,
    val selectedType: String = "Child",
    val name: String = "Oliver",
    val personality: String = "Curious and brave, loves exploring mysterious places and asking ‘what’s over there?’",
    val appearance: String = "Five years old with curly brown hair, bright green eyes, a yellow hoodie and blue sneakers.",
) {
    fun withName(value: String) = copy(name = value)

    fun withPersonality(value: String) = copy(personality = value)

    fun withAppearance(value: String) = copy(appearance = value)

    fun withType(value: String) = copy(selectedType = value)

    fun withErrors(
        name: UiText? = null,
        personality: UiText? = null,
        appearance: UiText? = null,
        type: UiText? = null,
    ) = copy(
        nameError = name,
        personalityError = personality,
        appearanceError = appearance,
        typeError = type,
    )
}
