package com.kjursa.foundation.compose.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/** Configurable Material 3 theme entry point for apps using Compose UI Kit. */
@Composable
fun FoundationTheme(
    colorScheme: ColorScheme = lightColorScheme(),
    typography: Typography = Typography(),
    shapes: Shapes = Shapes(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        shapes = shapes,
        content = content,
    )
}
