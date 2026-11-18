package com.dozy.api.client.auth

import com.dozy.api.client.auth.v1.AnonymousResult
import com.dozy.api.client.auth.v1.AuthClient
import com.dozy.api.client.auth.v1.LogoutResult
import com.dozy.api.client.auth.v1.RefreshResult
import com.dozy.api.client.auth.v1.VerifyEmailCodeResult
import com.dozy.api.contracts.v1.auth.AuthError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class StoredAuthSession(val ownerId: String, val accessToken: String, val refreshToken: String)

interface AuthSessionStorage {
    suspend fun load(): StoredAuthSession?
    suspend fun save(session: StoredAuthSession)
    suspend fun clear()
    suspend fun anonymousOwnerId(): String?
    suspend fun saveAnonymousOwnerId(ownerId: String)
    suspend fun clearAnonymousOwnerId()
}

class InMemoryAuthSessionStorage : AuthSessionStorage {
    private var session: StoredAuthSession? = null
    private var anonymousId: String? = null
    override suspend fun load() = session
    override suspend fun save(session: StoredAuthSession) { this.session = session }
    override suspend fun clear() { session = null; anonymousId = null }
    override suspend fun anonymousOwnerId() = anonymousId
    override suspend fun saveAnonymousOwnerId(ownerId: String) { anonymousId = ownerId }
    override suspend fun clearAnonymousOwnerId() { anonymousId = null }
}

sealed interface SessionState {
    data object SignedOut : SessionState
    data class Anonymous(val ownerId: String) : SessionState
    data class Authenticated(val ownerId: String) : SessionState
    data object Expired : SessionState
}

class AuthSessionManager internal constructor(
    private val auth: AuthClient,
    private val storage: AuthSessionStorage,
) {
    private val mutableState = MutableStateFlow<SessionState>(SessionState.SignedOut)
    val state: StateFlow<SessionState> = mutableState.asStateFlow()

    suspend fun initialize() {
        val session = storage.load()
        mutableState.value = session?.let { SessionState.Authenticated(it.ownerId) }
            ?: storage.anonymousOwnerId()?.let { SessionState.Anonymous(it) }
            ?: SessionState.SignedOut
    }

    suspend fun ensureAnonymous(): AnonymousResult {
        storage.anonymousOwnerId()?.let {
            mutableState.value = SessionState.Anonymous(it)
            return AnonymousResult.Success(it)
        }
        return when (val result = auth.anonymous()) {
            is AnonymousResult.Success -> {
                storage.saveAnonymousOwnerId(result.ownerId)
                mutableState.value = SessionState.Anonymous(result.ownerId)
                result
            }
            else -> result
        }
    }

    suspend fun requestEmailCode(email: String) = auth.requestEmailCode(email)

    suspend fun verifyEmailCode(sessionId: String, code: String): VerifyEmailCodeResult = when (val result = auth.verifyEmailCode(sessionId, code)) {
        is VerifyEmailCodeResult.Success -> {
            storage.save(StoredAuthSession(result.ownerId, result.accessToken, result.refreshToken))
            storage.clearAnonymousOwnerId()
            mutableState.value = SessionState.Authenticated(result.ownerId)
            result
        }
        else -> result
    }

    suspend fun refreshForRequest(): String? {
        val current = storage.load() ?: return null
        return when (val result = refresh(current.ownerId, current.refreshToken)) {
            is RefreshResult.Success -> {
                result.accessToken
            }
            else -> null
        }
    }

    suspend fun refresh(ownerId: String, refreshToken: String): RefreshResult = when (val result = auth.refresh(ownerId, refreshToken)) {
        is RefreshResult.Success -> {
            storage.save(StoredAuthSession(ownerId, result.accessToken, result.refreshToken))
            mutableState.value = SessionState.Authenticated(ownerId)
            result
        }
        is RefreshResult.ApiError -> {
            if (result.error == AuthError.InvalidRefreshToken || result.error == AuthError.SessionExpired) {
                storage.clear()
                mutableState.value = SessionState.Expired
            }
            result
        }
        is RefreshResult.TechnicalError -> result
    }

    suspend fun currentTokens() = storage.load()

    suspend fun logout(): LogoutResult {
        val current = storage.load()
        val result = current?.let { auth.logout(it.ownerId) } ?: LogoutResult.Success
        storage.clear()
        mutableState.value = SessionState.SignedOut
        return result
    }
}
