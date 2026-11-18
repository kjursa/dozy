package com.dozy.config

import io.ktor.server.config.ApplicationConfig
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

class AuthSecretProvider(secret: String) {
    init {
        require(secret.length >= 32) { "Auth secret must contain at least 32 characters" }
    }

    private val jwtSecret = hmac(secret.encodeToByteArray(), "v1:jwt")
    private val refreshSecret = hmac(secret.encodeToByteArray(), "v1:refresh")

    fun jwtSecret(period: String): SecretKey = SecretKeySpec(hmac(jwtSecret, period), ALGORITHM)

    fun hashRefreshToken(token: String): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(hmac(refreshSecret, token))

    companion object {
        private const val ALGORITHM = "HmacSHA256"
        private val development = AuthSecretProvider(
            Base64.getEncoder().encodeToString(ByteArray(32).also(SecureRandom()::nextBytes)),
        )

        fun from(config: ApplicationConfig): AuthSecretProvider =
            config.propertyOrNull("dozy.auth.secret")?.getString()?.takeIf(String::isNotBlank)
                ?.let(::AuthSecretProvider) ?: development

        private fun hmac(key: ByteArray, value: String): ByteArray =
            Mac.getInstance(ALGORITHM).run {
                init(SecretKeySpec(key, ALGORITHM))
                doFinal(value.encodeToByteArray())
            }
    }
}
