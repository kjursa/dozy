package com.dozy.features.auth.domain.repository

import com.dozy.features.auth.domain.model.CodeVerification
import com.dozy.features.auth.domain.model.EmailChallenge
import com.dozy.features.auth.domain.model.RefreshCredential

/** Each write operation is atomic, including its state checks. */
interface AuthRepository {
    suspend fun createAnonymous(
        ownerId: String,
        refresh: RefreshCredential,
    )

    suspend fun storeChallenge(
        challenge: EmailChallenge,
        countSince: Long,
        limit: Int,
    ): Boolean

    suspend fun verifyChallenge(
        sessionId: String,
        code: String,
        now: Long,
        maxAttempts: Int,
        newOwnerId: String,
        refresh: RefreshCredential,
    ): CodeVerification

    suspend fun rotateRefreshToken(
        ownerId: String,
        hashes: Set<String>,
        replacement: RefreshCredential,
        now: Long,
    ): Boolean

    suspend fun revokeRefreshTokens(ownerId: String)
}
