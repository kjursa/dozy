package com.dozy.mobile.feature.story.presentation.create

import com.dozy.mobile.feature.character.domain.model.Character
import com.kjursa.foundation.presentation.text.UiText
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/** Persist only draft input. A random setting is independent of automatic plot mode. */
@Serializable
internal data class CreateStoryUiState(
    @Transient val heroes: List<Character> = emptyList(),
    @Transient val characterLoading: Boolean = true,
    @Transient val characterLoadError: Boolean = false,
    @Transient val placeName: String? = null,
    val placeId: String? = null,
    val randomSetting: Boolean = true,
    val selectedHeroes: List<String> = emptyList(),
    val selectedLength: Int = 0,
    val specialRequest: String = "",
    val ending: String = "",
    val automaticPlot: Boolean = false,
    val requestKey: String? = null,
    val acceptedJobId: String? = null,
    @Transient val submitting: Boolean = false,
    @Transient val castError: UiText? = null,
    @Transient val lengthError: UiText? = null,
    @Transient val ideaError: UiText? = null,
    @Transient val endingError: UiText? = null,
    @Transient val submissionError: UiText? = null,
)
