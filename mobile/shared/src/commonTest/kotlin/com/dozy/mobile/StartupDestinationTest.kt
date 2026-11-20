package com.dozy.mobile

import com.dozy.api.client.auth.SessionState
import com.dozy.mobile.feature.launcher.presentation.LauncherDestination
import com.dozy.mobile.feature.launcher.presentation.resolveStartupDestination
import kotlin.test.Test
import kotlin.test.assertEquals

class StartupDestinationTest {
    @Test
    fun activeSessionsOpenMainRegardlessOfOnboardingState() {
        assertEquals(
            LauncherDestination.Home,
            resolveStartupDestination(SessionState.Authenticated("owner"), tutorialSeen = false),
        )
        assertEquals(
            LauncherDestination.Home,
            resolveStartupDestination(SessionState.Anonymous("owner"), tutorialSeen = true),
        )
    }

    @Test
    fun signedOutUsersContinueToTheAppropriateWelcomeScreen() {
        assertEquals(
            LauncherDestination.Tutorial,
            resolveStartupDestination(SessionState.SignedOut, tutorialSeen = false),
        )
        assertEquals(
            LauncherDestination.Welcome,
            resolveStartupDestination(SessionState.SignedOut, tutorialSeen = true),
        )
    }

    @Test
    fun expiredSessionsReturnToEmailLogin() {
        assertEquals(LauncherDestination.Email, resolveStartupDestination(SessionState.Expired, tutorialSeen = true))
    }
}
