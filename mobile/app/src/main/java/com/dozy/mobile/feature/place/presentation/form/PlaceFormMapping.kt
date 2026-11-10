package com.dozy.mobile.feature.place.presentation.form

import com.dozy.mobile.R
import com.dozy.mobile.feature.place.domain.model.PlaceErrors
import com.kjursa.foundation.presentation.text.UiText

internal fun PlaceErrors.Error?.toUiText(): UiText? =
    when (this) {
        PlaceErrors.Error.REQUIRED -> UiText.Resource(R.string.place_required)
        PlaceErrors.Error.TOO_LONG -> UiText.Resource(R.string.place_too_long)
        null -> null
    }
