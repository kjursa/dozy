package com.dozy.features.auth.domain.model

data class EmailChallenge(
    val sessionId: String,
    val email: String,
    val code: String,
    val createdAt: Long,
    val expiresAt: Long,
)

data class RefreshCredential(
    val hash: String,
    val expiresAt: Long,
)

sealed interface CodeVerification {
    data class Verified(
        val ownerId: String,
    ) : CodeVerification

    data object InvalidCode : CodeVerification

    data object TooManyAttempts : CodeVerification

    data object SessionExpired : CodeVerification
}
