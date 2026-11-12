package com.kjursa.foundation.presentation.text

import org.jetbrains.compose.resources.StringResource

/** Presentation text, resolved with the current locale only when rendered. */
sealed interface UiText {
    data class Resource(
        val id: StringResource,
        val arguments: List<Any> = emptyList(),
    ) : UiText

    data class Plain(
        val value: String,
    ) : UiText
}
