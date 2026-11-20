package com.dozy.mobile.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.dp
import com.dozy.mobile.ui.common.DozyLayout
import com.kjursa.foundation.compose.theme.FoundationTheme

private val DozyShapes =
    Shapes(
        small = RoundedCornerShape(DozyLayout.fieldRadius),
        medium = RoundedCornerShape(DozyLayout.cardRadius),
        large = RoundedCornerShape(DozyLayout.cardRadius),
        extraLarge = RoundedCornerShape(32.dp),
    )

@Composable
fun DozyTheme(content: @Composable () -> Unit) {
    FoundationTheme(
        colorScheme = DozyColorScheme,
        typography = DozyTypography,
        shapes = DozyShapes,
    ) {
        CompositionLocalProvider(LocalContentColor provides DozyColorScheme.onBackground, content = content)
    }
}
