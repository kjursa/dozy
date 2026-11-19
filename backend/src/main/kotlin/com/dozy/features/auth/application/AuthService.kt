package com.dozy.features.auth.application

import com.dozy.config.AuthSecretProvider
import com.dozy.domain.id.EmailSessionId
import com.dozy.domain.id.OwnerId
import com.dozy.domain.id.RefreshToken
import com.dozy.features.auth.domain.model.CodeVerification
import com.dozy.features.auth.domain.model.EmailChallenge
import com.dozy.features.auth.domain.model.RefreshCredential
import com.dozy.features.auth.domain.repository.AuthRepository
import java.time.Clock

class AuthService(
    private val repository: AuthRepository,
    private val secrets: AuthSecretProvider,
    private val accessTokens: AccessTokens,
    private val clock: Clock,
) {
    suspend fun anonymous(): TokenResult.Valid {
        val ownerId = OwnerId.generate()
        val refreshToken = RefreshToken.generate()
        repository.createAnonymous(ownerId, refreshCredential(refreshToken, clock.millis()))
        return tokens(ownerId, refreshToken)
    }

    suspend fun requestCode(emailValue: String): EmailRequest {
        val email = emailValue.trim().lowercase()
        if (!EMAIL.matches(email) || email.endsWith("@invalid.dozy")) return EmailRequest.InvalidEmail
        val now = clock.millis()
        val challenge = EmailChallenge(EmailSessionId.generate(), email, FAKE_CODE, now, now + CODE_LIFETIME)
        return if (repository.storeChallenge(challenge, countSince = now - HOUR, limit = 5)) {
            EmailRequest.Accepted(challenge.sessionId)
        } else {
            EmailRequest.RateLimited
        }
    }

    suspend fun verify(
        sessionId: EmailSessionId,
        code: String,
    ): VerifyResult {
        val now = clock.millis()
        val refreshToken = RefreshToken.generate()
        return when (
            val result =
                repository.verifyChallenge(
                    sessionId = sessionId,
                    code = code,
                    now = now,
                    maxAttempts = 3,
                    newOwnerId = OwnerId.generate(),
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
        ownerId: OwnerId,
        refreshToken: RefreshToken,
    ): TokenResult {
        val now = clock.millis()
        val replacement = RefreshToken.generate()
        val rotated =
            repository.rotateRefreshToken(
                ownerId = ownerId,
                hashes = secrets.refreshTokenLookupHashes(refreshToken),
                replacement = refreshCredential(replacement, now),
                now = now,
            )
        return if (rotated) tokens(ownerId, replacement) else TokenResult.Invalid
    }

    suspend fun logout(ownerId: OwnerId) = repository.revokeRefreshTokens(ownerId)

    private fun refreshCredential(
        token: RefreshToken,
        now: Long,
    ): RefreshCredential = RefreshCredential(secrets.hashRefreshToken(token), now + REFRESH_LIFETIME)

    private fun tokens(
        ownerId: OwnerId,
        refreshToken: RefreshToken,
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
        val sessionId: EmailSessionId,
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
        val ownerId: OwnerId,
        val accessToken: String,
        val refreshToken: RefreshToken,
    ) : TokenResult

    data object Invalid : TokenResult
}
