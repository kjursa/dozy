package com.dozy.features.auth

import com.dozy.config.AuthSecretProvider
import com.dozy.features.auth.application.AccessTokens
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AccessTokensTest {
    @Test
    fun acceptsCurrentOwnerAcrossKeyRotationUntilExpiry() {
        val secrets = AuthSecretProvider("test-signing-key-with-at-least-32-characters")
        val owner = "019a0000-0000-7000-8000-000000000003"
        val issuedAt = Instant.parse("2026-11-30T23:55:00Z")
        val token = AccessTokens(secrets, fixedClock(issuedAt)).issue(owner)

        assertEquals(owner, AccessTokens(secrets, fixedClock(issuedAt.plusSeconds(600))).ownerId(token))
        assertNull(AccessTokens(secrets, fixedClock(issuedAt.plusSeconds(1200))).ownerId(token))
        val otherSecrets = AuthSecretProvider("other-test-key-with-at-least-32-characters")
        assertNull(AccessTokens(otherSecrets, fixedClock(issuedAt)).ownerId(token))
    }

    @Test
    fun verifiesLegacyAccessTokens() {
        val secrets = AuthSecretProvider("test-signing-key-with-at-least-32-characters")
        val tokens = AccessTokens(secrets, fixedClock(Instant.parse("2026-12-01T00:00:00Z")))
        val legacyToken =
            "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCIsImtpZCI6IjIwMjYtMTEifQ." +
                "eyJzdWIiOiIwMTlhMDAwMC0wMDAwLTcwMDAtODAwMC0wMDAwMDAwMDAwMDMiLCJpc3MiOiJkb3p5IiwiYXVkIjoiZG96eS1tb2JpbGUiLCJpYXQiOjE3OTYwODI5MDAsImV4cCI6MTc5NjA4NDEwMH0." +
                "1ByG_cPawaO623QGS6-1SqNq4A3LgvXTAxxYv-OZG74"

        assertEquals(
            "019a0000-0000-7000-8000-000000000003",
            tokens.ownerId(legacyToken),
        )
    }

    private fun fixedClock(time: Instant): Clock = Clock.fixed(time, ZoneOffset.UTC)
}
