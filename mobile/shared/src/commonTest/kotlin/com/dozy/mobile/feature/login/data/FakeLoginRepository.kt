package com.dozy.mobile.feature.login.data

import com.dozy.mobile.feature.login.domain.repository.LoginRepository

class FakeLoginRepository : LoginRepository {
    private var pendingEmail: String? = null
    override suspend fun sendPin(email: String): Boolean { pendingEmail = email; return true }
    override suspend fun verifyPin(email: String, pin: String) = pendingEmail == email && pin.length == 9
    override suspend fun continueAsGuest() = true
}
