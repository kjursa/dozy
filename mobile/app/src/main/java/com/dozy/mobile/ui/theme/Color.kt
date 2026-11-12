package com.dozy.mobile.ui.theme

import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

internal val DozyColorScheme =
    lightColorScheme(
        primary = Color(0xFFFF6B6B),
        onPrimary = Color.White,
        secondary = Color(0xFF4ECDC4),
        onSecondary = Color.White,
        tertiary = Color(0xFFFFE66D),
        onTertiary = Color(0xFF2D3436),
        background = Color(0xFFFAF5EB),
        onBackground = Color(0xFF2D3436),
        surface = Color.White,
        onSurface = Color(0xFF2D3436),
        surfaceVariant = Color(0xFFF0E6D6),
        onSurfaceVariant = Color(0xFF636E72),
        outline = Color(0xFFEBDCC7),
        outlineVariant = Color(0xFFEBDCC7),
        error = Color(0xFFD63031),
        onError = Color.White,
    )

internal object DozyColors {
    val selectedBorder = Color(0xFFDB6963)
    val selectedBackground = Color(0xFFFFF6EE)
    val selectionControl = Color(0xFFD65F5A)
    val inspirationBackground = Color(0xFFE4F1E7)
    val onInspiration = Color(0xFF52695D)
    val inspirationAccent = Color(0xFF9A778E)
    val informationBackground = Color(0xFFF1EADC)
    val emptyStateBackground = Color(0xFFEEE9DF)
    val textLink = Color(0xFF267C76)
    val settingIconBackground = Color(0xFFE4EEE0)
    val settingIcon = Color(0xFF557E62)
    val navigationAccent = Color(0xFF568875)
    val placeholder = Color(0xFF868B88)
}
