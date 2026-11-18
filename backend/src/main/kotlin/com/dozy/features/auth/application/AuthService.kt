package com.dozy.features.auth.application

import com.dozy.config.AuthSecretProvider
import com.dozy.features.auth.domain.model.CodeVerification
import com.dozy.features.auth.domain.model.EmailChallenge
import com.dozy.features.auth.domain.model.RefreshCredential
import com.dozy.features.auth.domain.repository.AuthRepository
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class AuthService(
    private val repository: AuthRepository,
    private val secrets: AuthSecretProvider,
    private val accessTokens: AccessTokens,
) {
    suspend fun anonymous(): TokenResult.Valid {
        val ownerId = Uuid.generateV7().toString()
        val refreshToken = Uuid.random().toString()
        repository.createAnonymous(ownerId, refreshCredential(refreshToken, System.currentTimeMillis()))
        return tokens(ownerId, refreshToken)
    }

    suspend fun requestCode(emailValue: String): EmailRequest {
        val email = emailValue.trim().lowercase()
        if (!EMAIL.matches(email) || email.endsWith("@invalid.dozy")) return EmailRequest.InvalidEmail
        val now = System.currentTimeMillis()
        val challenge = EmailChallenge(Uuid.generateV7().toString(), email, FAKE_CODE, now, now + CODE_LIFETIME)
        return if (repository.storeChallenge(challenge, countSince = now - HOUR, limit = 5)) {
            EmailRequest.Accepted(challenge.sessionId)
        } else {
            EmailRequest.RateLimited
        }
    }

    suspend fun verify(
        sessionId: String,
        code: String,
    ): VerifyResult {
        val now = System.currentTimeMillis()
        val refreshToken = Uuid.random().toString()
        return when (
            val result =
                repository.verifyChallenge(
                    sessionId = sessionId,
                    code = code,
                    now = now,
                    maxAttempts = 3,
                    newOwnerId = Uuid.generateV7().toString(),
                    refresh = refreshCredential(refreshToken, now),
                )
        ) {
            is CodeVerification.Verified -> VerifyResult.Success(tokens(result.ownerId, refreshToken))
            CodeVerification.InvalidCode -> VerifyResult.InvalidCode
            CodeVerification.TooManyAttempts -> VerifyResult.TooManyAttempts
            CodeVerification.SessionExpired -> VerifyResult.SessionExpired
        }
    }

    suspend fun refresh(
        ownerId: String,
        refreshToken: String,
    ): TokenResult {
        val now = System.currentTimeMillis()
        val replacement = Uuid.random().toString()
        val rotated =
            repository.rotateRefreshToken(
                ownerId = ownerId,
                hashes = secrets.refreshTokenLookupHashes(refreshToken),
                replacement = refreshCredential(replacement, now),
                now = now,
            )
        return if (rotated) tokens(ownerId, replacement) else TokenResult.Invalid
    }

    suspend fun logout(ownerId: String) = repository.revokeRefreshTokens(ownerId)

    private fun refreshCredential(
        token: String,
        now: Long,
    ): RefreshCredential = RefreshCredential(secrets.hashRefreshToken(token), now + REFRESH_LIFETIME)

    private fun tokens(
        ownerId: String,
        refreshToken: String,
    ): TokenResult.Valid = TokenResult.Valid(ownerId, accessTokens.issue(ownerId), refreshToken)

    private companion object {
        const val FAKE_CODE = "123456789"
        const val HOUR = 60 * 60 * 1000L
        const val CODE_LIFETIME = 10 * 60 * 1000L
        const val REFRESH_LIFETIME = 180L * 24 * HOUR
        val EMAIL = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
    }
}

sealed interface EmailRequest {
    data class Accepted(
        val sessionId: String,
    ) : EmailRequest

    data object InvalidEmail : EmailRequest

    data object RateLimited : EmailRequest
}

sealed interface VerifyResult {
    data class Success(
        val tokens: TokenResult.Valid,
    ) : VerifyResult

    data object InvalidCode : VerifyResult

    data object TooManyAttempts : VerifyResult

    data object SessionExpired : VerifyResult
}

sealed interface TokenResult {
    data class Valid(
        val ownerId: String,
        val accessToken: String,
        val refreshToken: String,
    ) : TokenResult

    data object Invalid : TokenResult
}
