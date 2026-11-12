package com.kjursa.foundation.presentation.text

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource

@Composable
fun UiText.resolve(): String =
    when (this) {
        is UiText.Resource -> stringResource(id, *arguments.toTypedArray())
        is UiText.Plain -> value
    }
