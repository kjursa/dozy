package com.dozy.mobile

import com.dozy.api.client.auth.SessionState
import com.dozy.mobile.navigation.LOGIN_EMAIL_ROUTE
import com.dozy.mobile.navigation.LOGIN_TUTORIAL_ROUTE
import com.dozy.mobile.navigation.LOGIN_WELCOME_ROUTE
import com.dozy.mobile.navigation.MAIN_ROUTE
import com.dozy.mobile.navigation.resolveStartupDestination
import kotlin.test.Test
import kotlin.test.assertEquals

class StartupDestinationTest {
    @Test
    fun activeSessionsOpenMainRegardlessOfOnboardingState() {
        assertEquals(MAIN_ROUTE, resolveStartupDestination(SessionState.Authenticated("owner"), tutorialSeen = false))
        assertEquals(MAIN_ROUTE, resolveStartupDestination(SessionState.Anonymous("owner"), tutorialSeen = true))
    }

    @Test
    fun signedOutUsersContinueToTheAppropriateWelcomeScreen() {
        assertEquals(LOGIN_TUTORIAL_ROUTE, resolveStartupDestination(SessionState.SignedOut, tutorialSeen = false))
        assertEquals(LOGIN_WELCOME_ROUTE, resolveStartupDestination(SessionState.SignedOut, tutorialSeen = true))
    }

    @Test
    fun expiredSessionsReturnToEmailLogin() {
        assertEquals(LOGIN_EMAIL_ROUTE, resolveStartupDestination(SessionState.Expired, tutorialSeen = true))
    }
}
