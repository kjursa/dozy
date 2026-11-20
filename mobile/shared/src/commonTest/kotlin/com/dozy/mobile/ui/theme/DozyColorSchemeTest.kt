package com.dozy.mobile.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import kotlin.test.Test
import kotlin.test.assertTrue

class DozyColorSchemeTest {
    @Test
    fun primaryAndSecondaryTextMeetMinimumContrast() {
        val colors = DozyColorScheme

        assertTrue(contrastRatio(colors.primary, colors.onPrimary) >= 4.5, "Text on the primary color must meet 4.5:1 contrast")
        assertTrue(contrastRatio(colors.secondary, colors.onSecondary) >= 4.5, "Text on the secondary color must meet 4.5:1 contrast")
    }

    @Test
    fun brandBannerAndLinkTextMeetMinimumContrast() {
        val colors = DozyColorScheme

        assertTrue(contrastRatio(DozyColors.brandCoral, DozyColors.onBrandCoral) >= 4.5)
        assertTrue(contrastRatio(colors.background, DozyColors.textLink) >= 4.5)
        assertTrue(contrastRatio(colors.secondaryContainer, colors.onSecondaryContainer) >= 4.5)
    }

    @Test
    fun fieldFocusAndReaderNightTextRemainVisible() {
        assertTrue(contrastRatio(DozyColorScheme.surface, DozyColors.focus) >= 3.0)
        assertTrue(contrastRatio(DozyColorScheme.surface, DozyColors.fieldOutline) >= 3.0)
        assertTrue(contrastRatio(DozyColorScheme.surface, DozyColors.placeholder) >= 4.5)
        assertTrue(contrastRatio(DozyReaderNightColors.background, DozyReaderNightColors.onBackground) >= 4.5)
        assertTrue(contrastRatio(DozyReaderNightColors.primary, DozyReaderNightColors.onPrimary) >= 4.5)
    }

    private fun contrastRatio(
        background: Color,
        foreground: Color,
    ): Double {
        val backgroundLuminance = background.luminance()
        val foregroundLuminance = foreground.luminance()
        return (maxOf(backgroundLuminance, foregroundLuminance) + 0.05) /
            (minOf(backgroundLuminance, foregroundLuminance) + 0.05)
    }
}
