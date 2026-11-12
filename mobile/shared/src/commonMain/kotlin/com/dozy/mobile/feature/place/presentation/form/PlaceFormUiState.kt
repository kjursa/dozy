package com.dozy.mobile.feature.place.presentation.form

import com.kjursa.foundation.presentation.text.UiText
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class PlaceFormUiState(
    val id: String? = null,
    val initialized: Boolean = false,
    val name: String = "",
    val description: String = "",
    val inhabitants: String = "",
    @Transient val nameError: UiText? = null,
    @Transient val descriptionError: UiText? = null,
    @Transient val inhabitantsError: UiText? = null,
    @Transient val error: UiText? = null,
    @Transient val navigating: Boolean = false,
    @Transient val saving: Boolean = false,
    @Transient val loading: Boolean = false,
)
