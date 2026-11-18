package com.dozy.api.client.auth

import com.dozy.api.client.auth.v1.AnonymousResult
import com.dozy.api.client.auth.v1.KtorAuthClient
import com.dozy.api.client.auth.v1.LogoutResult
import com.dozy.api.client.auth.v1.RefreshResult
import com.dozy.api.client.auth.v1.VerifyEmailCodeResult
import com.dozy.api.contracts.v1.auth.AuthError
import com.dozy.api.client.communicate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class StoredAuthSession(
    val ownerId: String,
    val accessToken: String,
    val refreshToken: String,
    val anonymous: Boolean = false,
) {
    override fun toString(): String = "StoredAuthSession(anonymous=$anonymous)"
}

interface AuthSessionStorage {
    suspend fun load(): StoredAuthSession?
    suspend fun save(session: StoredAuthSession)
    suspend fun clear()
}

class InMemoryAuthSessionStorage : AuthSessionStorage {
    private var session: StoredAuthSession? = null
    override suspend fun load() = session
    override suspend fun save(session: StoredAuthSession) { this.session = session }
    override suspend fun clear() { session = null }
}

sealed interface SessionState {
    data object SignedOut : SessionState
    data class Anonymous(val ownerId: String) : SessionState
    data class Authenticated(val ownerId: String) : SessionState
    data object Expired : SessionState
}

class AuthSessionManager internal constructor(
    private val auth: KtorAuthClient,
    private val storage: AuthSessionStorage,
) {
    private val mutableState = MutableStateFlow<SessionState>(SessionState.SignedOut)
    val state: StateFlow<SessionState> = mutableState.asStateFlow()

    private val mutex = Mutex()

    suspend fun initialize() = mutex.withLock {
        mutableState.value = storage.load()?.state() ?: SessionState.SignedOut
    }

    internal suspend fun ensureAnonymous(): AnonymousResult = communicate(AnonymousResult::TechnicalError) {
        mutex.withLock {
            val current = storage.load()
            if (current?.anonymous == true) {
                return@withLock AnonymousResult.Success(current.ownerId, current.accessToken, current.refreshToken)
            }
            when (val result = auth.anonymous()) {
                is AnonymousResult.Success -> {
                    save(StoredAuthSession(result.ownerId, result.accessToken, result.refreshToken, anonymous = true))
                    result
                }
                else -> result
            }
        }
    }

    internal suspend fun verifyEmailCode(sessionId: String, code: String): VerifyEmailCodeResult =
        communicate(VerifyEmailCodeResult::TechnicalError) {
            mutex.withLock {
                when (val result = auth.verifyEmailCode(sessionId, code)) {
                    is VerifyEmailCodeResult.Success -> {
                        save(StoredAuthSession(result.ownerId, result.accessToken, result.refreshToken))
                        result
                    }
                    else -> result
                }
            }
        }

    internal suspend fun refreshForRequest(sent: StoredAuthSession): StoredAuthSession? = mutex.withLock {
        val current = storage.load() ?: return@withLock null
        if (current.ownerId != sent.ownerId) return@withLock null
        if (current != sent) return@withLock current
        refreshLocked(current.ownerId, current.refreshToken, current)
        storage.load()?.takeIf { it != current }
    }

    internal suspend fun refresh(ownerId: String, refreshToken: String): RefreshResult =
        communicate(RefreshResult::TechnicalError) {
            mutex.withLock { refreshLocked(ownerId, refreshToken, storage.load()) }
        }

    private suspend fun refreshLocked(ownerId: String, refreshToken: String, current: StoredAuthSession?): RefreshResult {
        val result = auth.refresh(ownerId, refreshToken)
        when {
            result is RefreshResult.Success && (current == null || current.ownerId == ownerId) -> {
                save(StoredAuthSession(ownerId, result.accessToken, result.refreshToken, current?.anonymous == true))
            }
            result is RefreshResult.ApiError && result.error == AuthError.InvalidRefreshToken &&
                current?.ownerId == ownerId && current.refreshToken == refreshToken -> {
                storage.clear()
                mutableState.value = SessionState.Expired
            }
        }
        return result
    }

    internal suspend fun currentTokens() = storage.load()

    suspend fun logout(): LogoutResult = communicate(LogoutResult::TechnicalError) {
        mutex.withLock {
            val current = storage.load() ?: return@withLock LogoutResult.Success
            var result = auth.logout(current.accessToken)
            if (result == LogoutResult.ApiError(AuthError.InvalidAccessToken)) {
                result = when (val refreshed = refreshLocked(current.ownerId, current.refreshToken, current)) {
                    is RefreshResult.Success -> auth.logout(refreshed.accessToken)
                    is RefreshResult.TechnicalError -> LogoutResult.TechnicalError(refreshed.error)
                    is RefreshResult.ApiError -> LogoutResult.ApiError(refreshed.error)
                }
            }
            if (result == LogoutResult.Success) {
                storage.clear()
                mutableState.value = SessionState.SignedOut
            }
            result
        }
    }

    private suspend fun save(session: StoredAuthSession) {
        storage.save(session)
        mutableState.value = session.state()
    }

    private fun StoredAuthSession.state(): SessionState =
        if (anonymous) SessionState.Anonymous(ownerId) else SessionState.Authenticated(ownerId)
}
