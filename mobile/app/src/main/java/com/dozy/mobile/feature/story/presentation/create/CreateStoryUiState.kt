package com.dozy.mobile.feature.story.presentation.create

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/** Persist only draft input. A random setting is independent of automatic plot mode. */
@Serializable
internal data class CreateStoryUiState(
    @Transient val placeName: String? = null,
    val placeId: String? = null,
    val randomSetting: Boolean = true,
    val selectedHeroes: List<String> = listOf("oliver", "captain_finn"),
    val selectedLength: Int = 0,
    val specialRequest: String = "",
    val ending: String = "",
    val automaticPlot: Boolean = false,
)
