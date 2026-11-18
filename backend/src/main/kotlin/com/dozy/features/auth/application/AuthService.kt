package com.dozy.features.auth.application

import com.dozy.features.auth.data.database.AuthEmailCodes
import com.dozy.features.auth.data.database.AuthRefreshTokens
import com.dozy.features.auth.data.database.AuthUsers
import com.dozy.infrastructure.database.SqliteDatabase
import java.util.Base64
import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update

class AuthService(private val database: SqliteDatabase, private val secret: String) {
    private val json = Json { encodeDefaults = true }

    init { require(secret.length >= 32) { "Auth secret must contain at least 32 characters" } }

    suspend fun anonymous(): String = database.write {
        val ownerId = uuidV7()
        AuthUsers.insert { it[AuthUsers.ownerId] = ownerId; it[AuthUsers.email] = "anonymous+$ownerId@invalid.dozy" }
        ownerId
    }

    suspend fun requestCode(emailValue: String): EmailRequest = database.write {
        val email = emailValue.trim().lowercase()
        if (!EMAIL.matches(email)) return@write EmailRequest.InvalidEmail
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
        val hash = refreshHash(refreshToken)
        val row = AuthRefreshTokens.selectAll().where { AuthRefreshTokens.tokenHash eq hash }.singleOrNull()
        if (row == null || row[AuthRefreshTokens.ownerId] != ownerId || row[AuthRefreshTokens.revoked] || row[AuthRefreshTokens.expiresAt] <= System.currentTimeMillis()) return@write TokenResult.Invalid
        AuthRefreshTokens.update({ AuthRefreshTokens.tokenHash eq hash }) { it[revoked] = true }
        createTokens(ownerId)
    }

    suspend fun logout(ownerId: String) = database.write {
        AuthRefreshTokens.update({ AuthRefreshTokens.ownerId eq ownerId }) { it[revoked] = true }
    }

    private fun JdbcTransaction.createTokens(ownerId: String): TokenResult.Valid {
        val now = System.currentTimeMillis()
        val refresh = UUID.randomUUID().toString()
        AuthRefreshTokens.insert {
            it[AuthRefreshTokens.tokenHash] = refreshHash(refresh)
            it[AuthRefreshTokens.ownerId] = ownerId
            it[AuthRefreshTokens.expiresAt] = now + REFRESH_LIFETIME
            it[AuthRefreshTokens.revoked] = false
        }
        return TokenResult.Valid(ownerId, accessToken(ownerId, now), refresh)
    }

    private fun accessToken(ownerId: String, now: Long): String {
        val header = encode(json.encodeToString(JwtHeader("HS256", "JWT", periodKey(now))))
        val payload = encode(json.encodeToString(JwtClaims(ownerId, ISSUER, AUDIENCE, now / 1000, now / 1000 + ACCESS_LIFETIME / 1000)))
        val input = "$header.$payload"
        val key = hmac(hmac(secret, "v1:jwt"), periodKey(now))
        return "$input.${Base64.getUrlEncoder().withoutPadding().encodeToString(hmac(key, input))}"
    }

    private fun refreshHash(token: String) = Base64.getUrlEncoder().withoutPadding().encodeToString(hmac(hmac(secret, "v1:refresh"), token))
    private fun periodKey(now: Long) = java.time.Instant.ofEpochMilli(now).atZone(java.time.ZoneOffset.UTC).let { "%04d-%02d".format(it.year, it.monthValue) }
    private fun encode(value: String) = Base64.getUrlEncoder().withoutPadding().encodeToString(value.encodeToByteArray())
    private fun hmac(key: String, value: String) = hmac(key.encodeToByteArray(), value)
    private fun hmac(key: ByteArray, value: String): ByteArray = Mac.getInstance("HmacSHA256").run { init(SecretKeySpec(key, "HmacSHA256")); doFinal(value.encodeToByteArray()) }

    private companion object {
        const val FAKE_CODE = "123456789"
        const val HOUR = 60 * 60 * 1000L
        const val CODE_LIFETIME = 10 * 60 * 1000L
        const val ACCESS_LIFETIME = 20 * 60 * 1000L
        const val REFRESH_LIFETIME = 180L * 24 * HOUR
        const val ISSUER = "dozy"
        const val AUDIENCE = "dozy-mobile"
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

@Serializable private data class JwtHeader(val alg: String, val typ: String, val kid: String)
@Serializable private data class JwtClaims(val sub: String, val iss: String, val aud: String, val iat: Long, val exp: Long)
