package com.dozy.mobile.feature.launcher.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dozy.api.client.auth.AuthSessionManager
import com.dozy.api.client.auth.SessionState
import com.dozy.mobile.navigation.AppNavigator
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class LauncherViewModel(
    private val sessionManager: AuthSessionManager,
    private val navigator: AppNavigator,
) : ViewModel() {
    private var startupJob: Job? = null
    private var startupResolved = false

    fun start(tutorialSeen: Boolean) {
        if (startupResolved || startupJob?.isActive == true) return
        startupJob = viewModelScope.launch {
            // Keep a saved session on Home; protected requests handle access-token expiry through refresh.
            sessionManager.initialize()
            navigator.navigate(resolveStartupDestination(sessionManager.state.value, tutorialSeen))
            startupResolved = true
        }
    }

    fun stop() {
        startupJob?.cancel()
        startupJob = null
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
