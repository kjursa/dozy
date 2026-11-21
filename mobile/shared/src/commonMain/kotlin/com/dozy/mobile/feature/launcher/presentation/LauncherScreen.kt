package com.dozy.mobile.feature.launcher.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
internal fun LauncherScreen(
    viewModel: LauncherViewModel,
    tutorialSeen: Boolean,
) {
    LifecycleStartEffect(viewModel, tutorialSeen) {
        viewModel.start(tutorialSeen)
        onStopOrDispose { viewModel.stop() }
    }
    Box(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}
