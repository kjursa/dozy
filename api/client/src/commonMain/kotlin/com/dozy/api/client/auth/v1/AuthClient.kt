package com.dozy.api.client.auth.v1

import com.dozy.api.client.TechnicalError as ClientTechnicalError
import com.dozy.api.contracts.v1.auth.AuthError

interface AuthClient {
    suspend fun anonymous(): AnonymousResult
    suspend fun requestEmailCode(email: String): RequestEmailCodeResult
    suspend fun verifyEmailCode(sessionId: String, code: String): VerifyEmailCodeResult
    suspend fun refresh(ownerId: String, refreshToken: String): RefreshResult
    suspend fun logout(): LogoutResult
}

sealed interface AnonymousResult {
    data class Success(val ownerId: String, val accessToken: String, val refreshToken: String) : AnonymousResult
    data class ApiError(val error: AuthError) : AnonymousResult
    data class TechnicalError(val error: ClientTechnicalError) : AnonymousResult
}
sealed interface RequestEmailCodeResult {
    data class Success(val sessionId: String) : RequestEmailCodeResult
    data class ApiError(val error: AuthError) : RequestEmailCodeResult
    data class TechnicalError(val error: ClientTechnicalError) : RequestEmailCodeResult
}
sealed interface VerifyEmailCodeResult {
    data class Success(val ownerId: String, val accessToken: String, val refreshToken: String) : VerifyEmailCodeResult
    data class ApiError(val error: AuthError) : VerifyEmailCodeResult
    data class TechnicalError(val error: ClientTechnicalError) : VerifyEmailCodeResult
}
sealed interface RefreshResult {
    data class Success(val accessToken: String, val refreshToken: String) : RefreshResult
    data class ApiError(val error: AuthError) : RefreshResult
    data class TechnicalError(val error: ClientTechnicalError) : RefreshResult
}
sealed interface LogoutResult {
    data object Success : LogoutResult
    data class ApiError(val error: AuthError) : LogoutResult
    data class TechnicalError(val error: ClientTechnicalError) : LogoutResult
}
