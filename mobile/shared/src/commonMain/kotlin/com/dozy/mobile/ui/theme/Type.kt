package com.dozy.mobile.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.dozy.mobile.resources.Res
import com.dozy.mobile.resources.jetbrains_mono
import com.dozy.mobile.resources.nunito_sans
import com.dozy.mobile.resources.playfair_display
import org.jetbrains.compose.resources.Font
import org.jetbrains.compose.resources.FontResource
import androidx.compose.ui.text.font.Font as ComposeFont

@OptIn(ExperimentalTextApi::class)
@Composable
private fun variableWeight(
    resourceId: FontResource,
    weight: Int,
): ComposeFont =
    Font(
        resource = resourceId,
        weight = FontWeight(weight),
        variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
    )

@OptIn(ExperimentalTextApi::class)
object DozyFontFamilies {
    val NunitoSans: FontFamily
        @Composable get() =
            FontFamily(
                variableWeight(Res.font.nunito_sans, 200),
                variableWeight(Res.font.nunito_sans, 300),
                variableWeight(Res.font.nunito_sans, 400),
                variableWeight(Res.font.nunito_sans, 500),
                variableWeight(Res.font.nunito_sans, 600),
                variableWeight(Res.font.nunito_sans, 700),
                variableWeight(Res.font.nunito_sans, 800),
                variableWeight(Res.font.nunito_sans, 900),
            )

    val PlayfairDisplay: FontFamily
        @Composable get() =
            FontFamily(
                variableWeight(Res.font.playfair_display, 400),
                variableWeight(Res.font.playfair_display, 500),
                variableWeight(Res.font.playfair_display, 600),
                variableWeight(Res.font.playfair_display, 700),
                variableWeight(Res.font.playfair_display, 800),
                variableWeight(Res.font.playfair_display, 900),
            )

    val JetBrainsMono: FontFamily
        @Composable get() =
            FontFamily(
                variableWeight(Res.font.jetbrains_mono, 100),
                variableWeight(Res.font.jetbrains_mono, 200),
                variableWeight(Res.font.jetbrains_mono, 300),
                variableWeight(Res.font.jetbrains_mono, 400),
                variableWeight(Res.font.jetbrains_mono, 500),
                variableWeight(Res.font.jetbrains_mono, 600),
                variableWeight(Res.font.jetbrains_mono, 700),
                variableWeight(Res.font.jetbrains_mono, 800),
            )
}

val DozyTypography: Typography
    @Composable get() {
        val nunito = DozyFontFamilies.NunitoSans
        val playfair = DozyFontFamilies.PlayfairDisplay
        return Typography(
            displayLarge = playfair.style(36, 44, FontWeight.Bold),
            displayMedium = nunito.style(32, 38, FontWeight.Black, -1f),
            displaySmall = nunito.style(28, 34, FontWeight.Black, -.5f),
            headlineLarge = nunito.style(34, 38, FontWeight.Black, -1f),
            headlineMedium = nunito.style(30, 34, FontWeight.Black, -1f),
            headlineSmall = nunito.style(22, 28, FontWeight.Black, -.5f),
            titleLarge = nunito.style(17, 23, FontWeight.Black),
            titleMedium = nunito.style(16, 22, FontWeight.Bold),
            titleSmall = nunito.style(14, 20, FontWeight.Bold),
            bodyLarge = nunito.style(15, 25, FontWeight.SemiBold),
            bodyMedium = nunito.style(13, 21, FontWeight.Normal),
            bodySmall = nunito.style(11, 19, FontWeight.Normal),
            labelLarge = nunito.style(14, 20, FontWeight.Black),
            labelMedium = nunito.style(12, 18, FontWeight.Bold),
            labelSmall = nunito.style(11, 17, FontWeight.Black, 1.3f),
        )
    }

private fun FontFamily.style(
    size: Int,
    lineHeight: Int,
    weight: FontWeight,
    tracking: Float = 0f,
): TextStyle =
    TextStyle(
        fontFamily = this,
        fontWeight = weight,
        fontSize = size.sp,
        lineHeight = lineHeight.sp,
        letterSpacing = tracking.sp,
    )
