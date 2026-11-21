package com.dozy.mobile.feature.launcher.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.kjursa.foundation.compose.mvi.MviScreen

@Composable
internal fun LauncherScreen(viewModel: LauncherViewModel) {
    MviScreen(viewModel) { _, _ ->
        Box(
            Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }
    }
}
