package com.dozy.mobile.feature.character.presentation.create

import com.dozy.mobile.feature.character.domain.model.CreateCharacterValidation
import com.kjursa.foundation.presentation.text.UiText
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
internal data class CreateCharacterUiState(
    @Transient val nameError: UiText? = null,
    @Transient val personalityError: UiText? = null,
    @Transient val appearanceError: UiText? = null,
    @Transient val typeError: UiText? = null,
    @Transient val saveStatus: SaveStatus = SaveStatus.EDITING,
    @Transient val loading: Boolean = false,
    @Transient val loadError: UiText? = null,
    @Transient val unavailable: Boolean = false,
    @Transient val generating: Boolean = false,
    @Transient val generationError: UiText? = null,
    val avatarDescription: List<String>? = null,
    val avatarId: String? = null,
    val avatarUrl: String? = null,
    val editingId: String? = null,
    val selectedType: String = "Child",
    val name: String? = null,
    val personality: String? = null,
    val appearance: String? = null,
) {
    val displayName: UiText get() = UiText.Plain(name.orEmpty())
    val displayPersonality: UiText get() = UiText.Plain(personality.orEmpty())
    val displayAppearance: UiText get() = UiText.Plain(appearance.orEmpty())
    val descriptionKey: List<String> get() = listOf(selectedType.uppercase(), personality.orEmpty().trim(), appearance.orEmpty().trim())
    val avatarOutdated: Boolean get() = avatarId != null && avatarDescription != descriptionKey
    val busy: Boolean get() = loading || generating || saveStatus == SaveStatus.SAVING
    val canSave: Boolean get() =
        !busy &&
            !unavailable &&
            loadError == null &&
            avatarId != null &&
            avatarUrl != null &&
            validateInput() is CreateCharacterValidation.Valid

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
