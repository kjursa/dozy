package com.dozy.mobile.feature.story.presentation.reader

import com.dozy.mobile.feature.story.domain.model.Story
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
internal data class ReaderUiState(
    val textSize: TextSize = TextSize.MEDIUM,
    val evening: Boolean = false,
    @Transient val loading: Boolean = true,
    @Transient val story: Story? = null,
    @Transient val unavailable: Boolean = false,
    @Transient val overlay: Overlay? = null,
) {
    @Serializable enum class TextSize { SMALL, MEDIUM, LARGE }

    enum class Overlay { SETTINGS, COVER }
}
