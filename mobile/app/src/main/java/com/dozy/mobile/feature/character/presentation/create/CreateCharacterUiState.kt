package com.dozy.mobile.feature.character.presentation.create

import com.dozy.mobile.R
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
    val selectedType: String = "Child",
    val name: String? = null,
    val personality: String? = null,
    val appearance: String? = null,
) {
    // Null means the resource-backed sample has not been edited or initialized yet.
    val displayName: UiText get() = name?.let(UiText::Plain) ?: UiText.Resource(R.string.sample_character_name)
    val displayPersonality: UiText get() = personality?.let(UiText::Plain) ?: UiText.Resource(R.string.sample_character_personality)
    val displayAppearance: UiText get() = appearance?.let(UiText::Plain) ?: UiText.Resource(R.string.sample_character_appearance)

    fun withSampleDefaults(
        name: String,
        personality: String,
        appearance: String,
    ) = copy(
        name = this.name ?: name,
        personality = this.personality ?: personality,
        appearance = this.appearance ?: appearance,
    )

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
