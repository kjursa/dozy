package com.dozy.mobile.presentation.createcharacter

internal data class CreateCharacterUiState(
    val errors: Map<CharacterField, String> = emptyMap(),
    val saveStatus: SaveStatus = SaveStatus.EDITING,
    val selectedType: String = "Child",
    val name: String = "Oliver",
    val personality: String = "Curious and brave, loves exploring mysterious places and asking ‘what’s over there?’",
    val appearance: String = "Five years old with curly brown hair, bright green eyes, a yellow hoodie and blue sneakers.",
)
