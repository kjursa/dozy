package com.dozy.mobile.feature.character.presentation.details

import com.dozy.mobile.feature.character.domain.model.Character
import com.kjursa.foundation.presentation.text.UiText

internal data class CharacterDetailsUiState(
    val loading: Boolean = true,
    val character: Character? = null,
    val readError: UiText? = null,
    val deleteDialog: Boolean = false,
    val deleting: Boolean = false,
    val deleteError: UiText? = null,
)
