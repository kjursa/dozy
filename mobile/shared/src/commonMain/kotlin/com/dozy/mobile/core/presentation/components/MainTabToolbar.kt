package com.dozy.mobile.core.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dozy.mobile.ui.theme.BottomTabPalette
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

private val toolbarHeight = 46.dp
private val dividerHeight = 1.dp

@Composable
internal fun MainTabLayout(
    title: String,
    palette: BottomTabPalette,
    leading: @Composable () -> Unit = {},
    trailing: @Composable () -> Unit = {},
    content: @Composable (topSpace: Dp) -> Unit,
) {
    val hazeState = rememberHazeState()
    val topSpace = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + toolbarHeight + dividerHeight
    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().hazeSource(hazeState).background(MaterialTheme.colorScheme.background)) {
            content(topSpace)
        }
        MainTabToolbar(title, palette, hazeState, leading, trailing)
    }
}

@Composable
private fun MainTabToolbar(
    title: String,
    palette: BottomTabPalette,
    hazeState: HazeState,
    leading: @Composable () -> Unit = {},
    trailing: @Composable () -> Unit = {},
) {
    val colors = MaterialTheme.colorScheme
    val tint = lerp(colors.background, palette.background, 0.5f)
    Column(
        Modifier
            .fillMaxWidth()
            .hazeEffect(
                state = hazeState,
                style =
                    HazeStyle(
                        backgroundColor = colors.background,
                        tint = HazeTint(tint.copy(alpha = 0.68f)),
                        fallbackTint = HazeTint(tint),
                        blurRadius = 16.dp,
                        noiseFactor = 0f,
                    ),
            ).statusBarsPadding(),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 22.dp).height(toolbarHeight),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) { leading() }
            Text(
                title.uppercase(),
                style =
                    MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        letterSpacing = 1.5.sp,
                        fontWeight = FontWeight.Black,
                    ),
            )
            Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) { trailing() }
        }
        HorizontalDivider(thickness = dividerHeight, color = colors.outlineVariant)
    }
}
