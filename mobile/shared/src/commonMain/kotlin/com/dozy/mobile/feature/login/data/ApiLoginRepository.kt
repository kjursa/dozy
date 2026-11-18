package com.dozy.mobile.feature.login.data

import com.dozy.api.client.DozyClient
import com.dozy.api.client.auth.v1.AnonymousResult
import com.dozy.api.client.auth.v1.RequestEmailCodeResult
import com.dozy.api.client.auth.v1.VerifyEmailCodeResult
import com.dozy.mobile.core.data.database.DozyDatabase
import com.dozy.mobile.feature.login.domain.repository.LoginRepository

class ApiLoginRepository(
    private val dozyClient: DozyClient,
    private val database: DozyDatabase,
) : LoginRepository {
    private var tutorialSeen = false
    private var sessionId: String? = null

    override fun hasSeenTutorial() = tutorialSeen
    override fun markTutorialSeen() {
        tutorialSeen = true
    }

    override suspend fun sendPin(email: String): Boolean =
        when (val result = dozyClient.auth.v1.requestEmailCode(email)) {
            is RequestEmailCodeResult.Success -> {
                sessionId = result.sessionId
                true
            }
            else -> false
        }

    override suspend fun verifyPin(email: String, pin: String): Boolean {
        val id = sessionId ?: return false
        return when (dozyClient.auth.v1.verifyEmailCode(id, pin)) {
            is VerifyEmailCodeResult.Success -> {
                database.clearAllTables()
                true
            }
            else -> false
        }
    }

    override suspend fun continueAsGuest(): Boolean {
        val success = dozyClient.auth.v1.anonymous() is AnonymousResult.Success
        if (success) tutorialSeen = true
        return success
    }
}
