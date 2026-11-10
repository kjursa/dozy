package com.dozy.mobile.presentation.text

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

@Composable
fun UiText.resolve(): String =
    when (this) {
        is UiText.Resource -> stringResource(id, *arguments.toTypedArray())
        is UiText.Plain -> value
    }
