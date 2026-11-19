package com.dozy.features.auth.domain.model

import com.dozy.domain.id.EmailSessionId
import com.dozy.domain.id.OwnerId

data class EmailChallenge(
    val sessionId: EmailSessionId,
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
        val ownerId: OwnerId,
    ) : CodeVerification

    data object InvalidCode : CodeVerification

    data object TooManyAttempts : CodeVerification

    data object SessionExpired : CodeVerification
}
