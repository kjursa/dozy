package com.dozy.mobile.core.presentation.text

import androidx.annotation.StringRes

/** Presentation text, resolved with the current locale only when rendered. */
sealed interface UiText {
    data class Resource(
        @param:StringRes @get:StringRes val id: Int,
        val arguments: List<Any> = emptyList(),
    ) : UiText

    data class Plain(
        val value: String,
    ) : UiText
}
