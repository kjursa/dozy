package com.dozy.mobile.feature.launcher.presentation

import androidx.lifecycle.viewModelScope
import com.dozy.api.client.auth.AuthSessionManager
import com.dozy.api.client.auth.SessionState
import com.dozy.mobile.feature.login.domain.repository.OnboardingPreferences
import com.dozy.mobile.navigation.AppNavigator
import com.kjursa.foundation.compose.mvi.MviViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class LauncherViewModel(
    private val sessionManager: AuthSessionManager,
    private val navigator: AppNavigator,
    private val onboardingPreferences: OnboardingPreferences,
) : MviViewModel<Unit, Unit, Nothing>(Unit) {
    private var startupJob: Job? = null
    private var startupResolved = false

    override fun onAction(action: Unit) = Unit

    override fun start() {
        if (startupResolved || startupJob?.isActive == true) return
        startupJob = viewModelScope.launch {
            // Keep a saved session on Home; protected requests handle access-token expiry through refresh.
            sessionManager.initialize()
            val tutorialSeen = onboardingPreferences.hasSeenTutorial()
            navigator.navigate(resolveStartupDestination(sessionManager.state.value, tutorialSeen))
            startupResolved = true
        }
    }

    override fun stop() {
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
