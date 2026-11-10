package com.dozy.mobile.presentation.createcharacter

import com.dozy.mobile.presentation.text.UiText

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
)
