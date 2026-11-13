package com.dozy.mobile.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.dozy.mobile.resources.*
import org.jetbrains.compose.resources.Font

@OptIn(ExperimentalTextApi::class)
@androidx.compose.runtime.Composable
private fun variableWeight(
    resourceId: org.jetbrains.compose.resources.FontResource,
    weight: Int,
): androidx.compose.ui.text.font.Font =
    Font(
        resource = resourceId,
        weight = FontWeight(weight),
        variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
    )

@OptIn(ExperimentalTextApi::class)
object DozyFontFamilies {
    val NunitoSans: FontFamily
        @androidx.compose.runtime.Composable get() =
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
        @androidx.compose.runtime.Composable get() =
            FontFamily(
                variableWeight(Res.font.playfair_display, 400),
                variableWeight(Res.font.playfair_display, 500),
                variableWeight(Res.font.playfair_display, 600),
                variableWeight(Res.font.playfair_display, 700),
                variableWeight(Res.font.playfair_display, 800),
                variableWeight(Res.font.playfair_display, 900),
            )

    val JetBrainsMono: FontFamily
        @androidx.compose.runtime.Composable get() =
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
    @androidx.compose.runtime.Composable get() =
        Typography(
            displayLarge =
                TextStyle(
                    fontFamily = DozyFontFamilies.PlayfairDisplay,
                    fontWeight = FontWeight.Bold,
                    fontSize = 36.sp,
                    lineHeight = 44.sp,
                ),
            headlineLarge =
                TextStyle(
                    fontFamily = DozyFontFamilies.NunitoSans,
                    fontWeight = FontWeight.Black,
                    fontSize = 30.sp,
                    lineHeight = 36.sp,
                ),
            headlineSmall =
                TextStyle(
                    fontFamily = DozyFontFamilies.NunitoSans,
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp,
                    lineHeight = 25.sp,
                ),
            titleLarge =
                TextStyle(
                    fontFamily = DozyFontFamilies.NunitoSans,
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp,
                    lineHeight = 20.sp,
                ),
            titleMedium =
                TextStyle(
                    fontFamily = DozyFontFamilies.NunitoSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                ),
            bodyLarge =
                TextStyle(
                    fontFamily = DozyFontFamilies.NunitoSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                ),
            bodyMedium =
                TextStyle(
                    fontFamily = DozyFontFamilies.NunitoSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                ),
            bodySmall =
                TextStyle(
                    fontFamily = DozyFontFamilies.NunitoSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                ),
            labelLarge =
                TextStyle(
                    fontFamily = DozyFontFamilies.NunitoSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                ),
            labelSmall =
                TextStyle(
                    fontFamily = DozyFontFamilies.NunitoSans,
                    fontWeight = FontWeight.Black,
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    letterSpacing = 1.3.sp,
                ),
        )
