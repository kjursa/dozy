package com.dozy.features.auth.application

import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Instant
import java.time.ZoneOffset
import java.util.Base64
import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class AccessTokens(secret: String) {
    private val key = hmac(secret.encodeToByteArray(), "v1:jwt")

    fun issue(ownerId: String, now: Instant = Instant.now()): String {
        val period = periodKey(now)
        val header = encode(Json.encodeToString(JwtHeader("HS256", "JWT", period)))
        val payload = encode(Json.encodeToString(JwtClaims(ownerId, ISSUER, AUDIENCE, now.epochSecond, now.epochSecond + LIFETIME)))
        val input = "$header.$payload"
        return "$input.${encoder.encodeToString(sign(input, period))}"
    }

    fun ownerId(token: String, now: Instant = Instant.now()): String? {
        if (token.length > 2048) return null
        val segments = token.split('.')
        if (segments.size != 3) return null
        return try {
            val (header, payload, signature) = segments
            val period = Json.decodeFromString<JwtHeader>(decode(header))
            if (period.alg != "HS256" || period.typ != "JWT") return null
            if (period.kid != periodKey(now) && period.kid != periodKey(now.minusSeconds(LIFETIME))) return null
            if (!MessageDigest.isEqual(sign("$header.$payload", period.kid), decoder.decode(signature))) return null
            val claims = Json.decodeFromString<JwtClaims>(decode(payload))
            if (claims.iss != ISSUER || claims.aud != AUDIENCE) return null
            if (claims.iat > now.epochSecond || claims.iat < now.epochSecond - LIFETIME ||
                claims.exp != claims.iat + LIFETIME || claims.exp <= now.epochSecond ||
                period.kid != periodKey(Instant.ofEpochSecond(claims.iat))
            ) return null
            claims.sub.takeIf { UUID.fromString(it).toString() == it }
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    private fun sign(input: String, period: String) = hmac(hmac(key, period), input)
    private fun periodKey(now: Instant) = now.atZone(ZoneOffset.UTC).toLocalDate().toString().take(7)
    private fun encode(value: String) = encoder.encodeToString(value.encodeToByteArray())
    private fun decode(value: String) = decoder.decode(value).decodeToString()

    private companion object {
        const val LIFETIME = 20 * 60L
        const val ISSUER = "dozy"
        const val AUDIENCE = "dozy-mobile"
        val encoder = Base64.getUrlEncoder().withoutPadding()
        val decoder = Base64.getUrlDecoder()
    }
}

internal fun hmac(key: ByteArray, value: String): ByteArray =
    Mac.getInstance("HmacSHA256").run {
        init(SecretKeySpec(key, "HmacSHA256"))
        doFinal(value.encodeToByteArray())
    }

internal val developmentAuthSecret: String =
    Base64.getEncoder().encodeToString(ByteArray(32).also(SecureRandom()::nextBytes))

@Serializable
private data class JwtHeader(val alg: String, val typ: String, val kid: String)

@Serializable
private data class JwtClaims(val sub: String, val iss: String, val aud: String, val iat: Long, val exp: Long)
