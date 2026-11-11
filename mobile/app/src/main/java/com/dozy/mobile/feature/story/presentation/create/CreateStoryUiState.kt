package com.dozy.mobile.feature.story.presentation.create

import com.dozy.mobile.feature.character.domain.model.Character
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/** Persist only draft input. A random setting is independent of automatic plot mode. */
@Serializable
internal data class CreateStoryUiState(
    @Transient val heroes: List<Character> = emptyList(),
    @Transient val characterLoadError: Boolean = false,
    @Transient val placeName: String? = null,
    val placeId: String? = null,
    val randomSetting: Boolean = true,
    val selectedHeroes: List<String> = emptyList(),
    val selectedLength: Int = 0,
    val specialRequest: String = "",
    val ending: String = "",
    val automaticPlot: Boolean = false,
)
