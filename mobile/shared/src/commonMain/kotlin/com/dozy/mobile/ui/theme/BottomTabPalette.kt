package com.dozy.mobile.ui.theme

import androidx.compose.ui.graphics.Color

internal enum class BottomTabPalette(
    val active: Color,
    val background: Color,
) {
    HOME(Color(0xFF226D67), Color(0xFFE7F5EF)),
    STORIES(Color(0xFF77546F), Color(0xFFF3E7F0)),
    CHARACTERS(Color(0xFFAF4C43), Color(0xFFFFEDE7)),
}
