package com.dozy.mobile.feature.login.domain.repository

interface LoginRepository {
    suspend fun sendPin(email: String): Boolean

    suspend fun verifyPin(
        email: String,
        pin: String,
    ): Boolean

    suspend fun continueAsGuest(): Boolean
}
