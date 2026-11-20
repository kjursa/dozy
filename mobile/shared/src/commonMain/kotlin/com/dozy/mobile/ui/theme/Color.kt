package com.dozy.mobile.ui.theme

import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

private val Charcoal = Color(0xFF2D3436)

val DozyColorScheme =
    lightColorScheme(
        primary = Color(0xFFBE5150),
        onPrimary = Color.White,
        secondary = Color(0xFF226D67),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFE7F5EF),
        onSecondaryContainer = Color(0xFF226D67),
        tertiary = Color(0xFFFFE66D),
        onTertiary = Charcoal,
        background = Color(0xFFFAF5EB),
        onBackground = Charcoal,
        surface = Color.White,
        onSurface = Charcoal,
        surfaceVariant = Color(0xFFF0E6D6),
        onSurfaceVariant = Color(0xFF636E72),
        outline = Color(0xFFEBDCC7),
        outlineVariant = Color(0xFFEBDCC7),
        error = Color(0xFFD63031),
        onError = Color.White,
    )

internal val DozyReaderNightColors =
    DozyColorScheme.copy(
        primary = Color(0xFFFF9990),
        onPrimary = Color(0xFF202B30),
        background = Color(0xFF202B30),
        onBackground = Color(0xFFEEE4D1),
        surface = Color(0xFF2B393F),
        onSurface = Color(0xFFEEE4D1),
        surfaceVariant = Color(0xFF354147),
        onSurfaceVariant = Color(0xFFB8B9B1),
        surfaceContainerHigh = Color(0xFF354147),
        outline = Color(0xFF435054),
        outlineVariant = Color(0xFF435054),
        secondary = Color(0xFF9ACAC0),
        onSecondary = Color(0xFF202B30),
    )

internal object DozyColors {
    val brandCoral = Color(0xFFFF6B6B)
    val onBrandCoral = Charcoal
    val brandTeal = Color(0xFF4ECDC4)
    val focus = Color(0xFF267C76)
    val fieldOutline = Color(0xFF8D8476)
    val selectedBorder = Color(0xFFDB6963)
    val selectedBackground = Color(0xFFFFF6EE)
    val selectionControl = Color(0xFFD65F5A)
    val inspirationBackground = Color(0xFFE4F1E7)
    val onInspiration = Color(0xFF52695D)
    val inspirationAccent = Color(0xFF9A778E)
    val informationBackground = Color(0xFFF1EADC)
    val emptyStateBackground = Color(0xFFEEE9DF)
    val textLink = Color(0xFF226D67)
    val dangerLink = Color(0xFFAF4C43)
    val settingIconBackground = Color(0xFFE4EEE0)
    val settingIcon = Color(0xFF557E62)
    val navigationAccent = Color(0xFF568875)
    val placeholder = Color(0xFF636E72)
}
