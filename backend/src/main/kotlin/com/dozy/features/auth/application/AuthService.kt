package com.dozy.features.auth.application

import com.dozy.features.auth.data.database.AuthEmailCodes
import com.dozy.features.auth.data.database.AuthRefreshTokens
import com.dozy.features.auth.data.database.AuthUsers
import com.dozy.infrastructure.database.SqliteDatabase
import com.dozy.config.AuthSecretProvider
import java.util.UUID
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update

class AuthService(
    private val database: SqliteDatabase,
    private val secrets: AuthSecretProvider,
    private val accessTokens: AccessTokens,
) {

    suspend fun anonymous(): TokenResult.Valid = database.write {
        val ownerId = uuidV7()
        AuthUsers.insert { it[AuthUsers.ownerId] = ownerId; it[AuthUsers.email] = "anonymous+$ownerId@invalid.dozy" }
        createTokens(ownerId)
    }

    suspend fun requestCode(emailValue: String): EmailRequest = database.write {
        val email = emailValue.trim().lowercase()
        if (!EMAIL.matches(email) || email.endsWith("@invalid.dozy")) return@write EmailRequest.InvalidEmail
        val now = System.currentTimeMillis()
        val recentCount = AuthEmailCodes.selectAll().where { (AuthEmailCodes.email eq email) and (AuthEmailCodes.createdAt greaterEq now - HOUR) }.count()
        if (recentCount >= 5) return@write EmailRequest.RateLimited
        val session = uuidV7()
        AuthEmailCodes.insert {
            it[AuthEmailCodes.sessionId] = session
            it[AuthEmailCodes.email] = email
            it[AuthEmailCodes.code] = FAKE_CODE
            it[AuthEmailCodes.createdAt] = now
            it[AuthEmailCodes.expiresAt] = now + CODE_LIFETIME
            it[AuthEmailCodes.attempts] = 0
            it[AuthEmailCodes.consumed] = false
        }
        EmailRequest.Accepted(session)
    }

    suspend fun verify(sessionId: String, code: String): VerifyResult = database.write {
        val row = AuthEmailCodes.selectAll().where { AuthEmailCodes.sessionId eq sessionId }.singleOrNull()
            ?: return@write VerifyResult.SessionExpired
        if (row[AuthEmailCodes.consumed] || row[AuthEmailCodes.expiresAt] <= System.currentTimeMillis()) return@write VerifyResult.SessionExpired
        if (row[AuthEmailCodes.attempts] >= 3) return@write VerifyResult.TooManyAttempts
        if (code != row[AuthEmailCodes.code]) {
            AuthEmailCodes.update({ AuthEmailCodes.sessionId eq sessionId }) { it[attempts] = row[AuthEmailCodes.attempts] + 1 }
            return@write if (row[AuthEmailCodes.attempts] + 1 >= 3) VerifyResult.TooManyAttempts else VerifyResult.InvalidCode
        }
        AuthEmailCodes.update({ AuthEmailCodes.sessionId eq sessionId }) { it[consumed] = true }
        val email = row[AuthEmailCodes.email]
        val existing = AuthUsers.selectAll().where { AuthUsers.email eq email }.singleOrNull()
        val ownerId = existing?.get(AuthUsers.ownerId) ?: uuidV7().also { id ->
            AuthUsers.insert { it[AuthUsers.ownerId] = id; it[AuthUsers.email] = email }
        }
        VerifyResult.Success(createTokens(ownerId))
    }

    suspend fun refresh(ownerId: String, refreshToken: String): TokenResult = database.write {
        val hash = secrets.hashRefreshToken(refreshToken)
        val row = AuthRefreshTokens.selectAll().where { AuthRefreshTokens.tokenHash eq hash }.singleOrNull()
        if (row == null || row[AuthRefreshTokens.ownerId] != ownerId || row[AuthRefreshTokens.revoked] || row[AuthRefreshTokens.expiresAt] <= System.currentTimeMillis()) return@write TokenResult.Invalid
        AuthRefreshTokens.update({ AuthRefreshTokens.tokenHash eq hash }) { it[revoked] = true }
        createTokens(row[AuthRefreshTokens.ownerId])
    }

    suspend fun logout(ownerId: String) = database.write {
        AuthRefreshTokens.update({ AuthRefreshTokens.ownerId eq ownerId }) { it[revoked] = true }
    }

    private fun JdbcTransaction.createTokens(ownerId: String): TokenResult.Valid {
        val now = System.currentTimeMillis()
        val refresh = UUID.randomUUID().toString()
        AuthRefreshTokens.insert {
            it[AuthRefreshTokens.tokenHash] = secrets.hashRefreshToken(refresh)
            it[AuthRefreshTokens.ownerId] = ownerId
            it[AuthRefreshTokens.expiresAt] = now + REFRESH_LIFETIME
            it[AuthRefreshTokens.revoked] = false
        }
        return TokenResult.Valid(ownerId, accessTokens.issue(ownerId), refresh)
    }

    private companion object {
        const val FAKE_CODE = "123456789"
        const val HOUR = 60 * 60 * 1000L
        const val CODE_LIFETIME = 10 * 60 * 1000L
        const val REFRESH_LIFETIME = 180L * 24 * HOUR
        val EMAIL = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
        fun uuidV7(): String {
            val bytes = ByteArray(16).also { java.security.SecureRandom().nextBytes(it) }
            val time = System.currentTimeMillis()
            for (i in 0..5) bytes[i] = (time ushr (40 - i * 8)).toByte()
            bytes[6] = ((bytes[6].toInt() and 0x0f) or 0x70).toByte()
            bytes[8] = ((bytes[8].toInt() and 0x3f) or 0x80).toByte()
            val buffer = java.nio.ByteBuffer.wrap(bytes)
            return UUID(buffer.long, buffer.long).toString()
        }
    }
}

sealed interface EmailRequest { data class Accepted(val sessionId: String) : EmailRequest; data object InvalidEmail : EmailRequest; data object RateLimited : EmailRequest }
sealed interface VerifyResult { data class Success(val tokens: TokenResult.Valid) : VerifyResult; data object InvalidCode : VerifyResult; data object TooManyAttempts : VerifyResult; data object SessionExpired : VerifyResult }
sealed interface TokenResult { data class Valid(val ownerId: String, val accessToken: String, val refreshToken: String) : TokenResult; data object Invalid : TokenResult }
