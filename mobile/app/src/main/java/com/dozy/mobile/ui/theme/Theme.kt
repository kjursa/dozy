package com.dozy.mobile.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.kjursa.composeuikit.theme.ComposeUiKitTheme

private val DozyShapes =
    Shapes(
        small = RoundedCornerShape(16.dp),
        medium = RoundedCornerShape(24.dp),
        large = RoundedCornerShape(32.dp),
        extraLarge = RoundedCornerShape(32.dp),
    )

@Composable
fun DozyTheme(content: @Composable () -> Unit) {
    ComposeUiKitTheme(
        colorScheme = DozyColorScheme,
        typography = DozyTypography,
        shapes = DozyShapes,
        content = content,
    )
}
