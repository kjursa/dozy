package com.dozy.mobile.core.presentation.layout

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
internal fun mainBottomMenuScrollSpace(): Dp = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 122.dp
