package com.dozy.api.contracts.v1.auth

import com.dozy.api.contracts.v1.ApiBusinessError
import com.dozy.api.contracts.v1.businessErrorSerializer
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable

@Serializable(with = AuthErrorSerializer::class)
sealed interface AuthError : ApiBusinessError {
    data object InvalidEmail : AuthError { override val code = "INVALID_EMAIL" }
    data object InvalidCode : AuthError { override val code = "INVALID_CODE" }
    data object SessionExpired : AuthError { override val code = "AUTH_SESSION_EXPIRED" }
    data object TooManyAttempts : AuthError { override val code = "AUTH_TOO_MANY_ATTEMPTS" }
    data object RateLimited : AuthError { override val code = "AUTH_RATE_LIMITED" }
    data object InvalidRefreshToken : AuthError { override val code = "INVALID_REFRESH_TOKEN" }
    data class Unknown(override val code: String) : AuthError

    companion object {
        val known: List<AuthError> = listOf(InvalidEmail, InvalidCode, SessionExpired, TooManyAttempts, RateLimited, InvalidRefreshToken)
    }
}

object AuthErrorSerializer : KSerializer<AuthError> by businessErrorSerializer(AuthError.known, AuthError::Unknown)
