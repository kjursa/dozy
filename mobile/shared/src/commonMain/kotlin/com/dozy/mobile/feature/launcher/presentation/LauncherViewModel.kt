package com.dozy.mobile.feature.launcher.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dozy.api.client.auth.AuthSessionManager
import com.dozy.api.client.auth.SessionState
import com.dozy.mobile.navigation.AppNavigator
import kotlinx.coroutines.launch

class LauncherViewModel(
    private val sessionManager: AuthSessionManager,
    private val navigator: AppNavigator,
) : ViewModel() {
    private var startupJobStarted = false

    fun start(tutorialSeen: Boolean) {
        if (startupJobStarted) return
        startupJobStarted = true
        viewModelScope.launch {
            // Keep a saved session on Home; protected requests handle access-token expiry through refresh.
            sessionManager.initialize()
            navigator.navigate(resolveStartupDestination(sessionManager.state.value, tutorialSeen))
        }
    }
}

internal fun resolveStartupDestination(
    sessionState: SessionState,
    tutorialSeen: Boolean,
): LauncherDestination =
    when (sessionState) {
        is SessionState.Authenticated, is SessionState.Anonymous -> LauncherDestination.Home
        SessionState.Expired -> LauncherDestination.Email
        SessionState.SignedOut -> if (tutorialSeen) LauncherDestination.Welcome else LauncherDestination.Tutorial
    }
