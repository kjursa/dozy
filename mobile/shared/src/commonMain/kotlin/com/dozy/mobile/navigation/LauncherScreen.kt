package com.dozy.mobile.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.dozy.api.client.auth.SessionState

@Composable
internal fun LauncherScreen() {
    Box(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

internal fun resolveStartupDestination(
    sessionState: SessionState,
    tutorialSeen: Boolean,
): String =
    when (sessionState) {
        is SessionState.Authenticated, is SessionState.Anonymous -> MAIN_ROUTE
        SessionState.Expired -> LOGIN_EMAIL_ROUTE
        SessionState.SignedOut -> if (tutorialSeen) LOGIN_WELCOME_ROUTE else LOGIN_TUTORIAL_ROUTE
    }
