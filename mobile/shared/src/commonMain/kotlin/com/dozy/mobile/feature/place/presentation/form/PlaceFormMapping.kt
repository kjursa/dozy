package com.dozy.mobile.feature.place.presentation.form

import com.dozy.mobile.feature.place.domain.model.PlaceErrors
import com.dozy.mobile.resources.*
import com.kjursa.foundation.presentation.text.UiText

fun PlaceErrors.Error?.toUiText(): UiText? =
    when (this) {
        PlaceErrors.Error.REQUIRED -> UiText.Resource(Res.string.place_required)
        PlaceErrors.Error.TOO_LONG -> UiText.Resource(Res.string.place_too_long)
        null -> null
    }
