package com.dozy.mobile.feature.login.data

import com.dozy.mobile.feature.login.domain.repository.LoginRepository
import kotlinx.coroutines.delay

/** In-memory auth placeholder for the UI flow; it performs no persistence or network requests. */
class FakeLoginRepository : LoginRepository {
    private var tutorialSeen = false
    private var pendingEmail: String? = null

    override fun hasSeenTutorial(): Boolean = tutorialSeen

    override fun markTutorialSeen() {
        tutorialSeen = true
    }

    override suspend fun sendPin(email: String) {
        pendingEmail = email
    }

    override suspend fun verifyPin(
        email: String,
        pin: String,
    ): Boolean {
        delay(3_000)
        return pendingEmail == email && pin.length == PIN_LENGTH
    }

    override suspend fun continueAsGuest() {
        tutorialSeen = true
        pendingEmail = null
    }

    private companion object {
        const val PIN_LENGTH = 9
    }
}
