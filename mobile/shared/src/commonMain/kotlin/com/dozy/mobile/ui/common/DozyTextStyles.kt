package com.dozy.mobile.ui.common

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.dozy.mobile.ui.theme.DozyFontFamilies

internal object DozyTextStyles {
    val bannerTitle: TextStyle
        @Composable get() = MaterialTheme.typography.headlineSmall.copy(fontSize = 26.sp, lineHeight = 33.sp)

    val readerTitle: TextStyle
        @Composable get() = MaterialTheme.typography.headlineLarge.copy(fontSize = 37.sp, lineHeight = 42.sp)

    val readerEnding: TextStyle
        @Composable get() =
            TextStyle(
                fontFamily = DozyFontFamilies.PlayfairDisplay,
                fontWeight = FontWeight.Medium,
                fontSize = 27.sp,
                lineHeight = 34.sp,
            )

    @Composable
    fun readerBody(size: Int): TextStyle =
        TextStyle(
            fontFamily = DozyFontFamilies.PlayfairDisplay,
            fontWeight = FontWeight.Normal,
            fontSize = size.sp,
            lineHeight = (size * 1.95f).sp,
            letterSpacing = .05.sp,
        )
}
