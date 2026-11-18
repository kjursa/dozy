package com.dozy.api.client.auth

import com.dozy.api.client.auth.v1.AnonymousResult
import com.dozy.api.client.auth.v1.AuthClient
import com.dozy.api.client.auth.v1.LogoutResult
import com.dozy.api.client.auth.v1.RefreshResult
import com.dozy.api.client.auth.v1.RequestEmailCodeResult
import com.dozy.api.client.auth.v1.VerifyEmailCodeResult

internal class ManagedAuthClient(
    private val delegate: AuthClient,
    private val sessionManager: AuthSessionManager,
) : AuthClient {
    override suspend fun anonymous(): AnonymousResult = sessionManager.ensureAnonymous()

    override suspend fun requestEmailCode(email: String): RequestEmailCodeResult = delegate.requestEmailCode(email)

    override suspend fun verifyEmailCode(sessionId: String, code: String): VerifyEmailCodeResult =
        sessionManager.verifyEmailCode(sessionId, code)

    override suspend fun refresh(ownerId: String, refreshToken: String): RefreshResult =
        sessionManager.refresh(ownerId, refreshToken)

    override suspend fun logout(ownerId: String): LogoutResult = sessionManager.logout()
}
