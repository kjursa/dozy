package com.dozy.features.auth

import com.dozy.features.auth.application.AccessTokens
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AccessTokensTest {
    @Test
    fun acceptsCurrentOwnerAcrossKeyRotationUntilExpiry() {
        val tokens = AccessTokens("test-signing-key-with-at-least-32-characters")
        val owner = "019a0000-0000-7000-8000-000000000003"
        val issuedAt = Instant.parse("2026-11-30T23:55:00Z")
        val token = tokens.issue(owner, issuedAt)

        assertEquals(owner, tokens.ownerId(token, issuedAt.plusSeconds(600)))
        assertNull(tokens.ownerId(token, issuedAt.plusSeconds(1200)))
        assertNull(AccessTokens("other-test-key-with-at-least-32-characters").ownerId(token, issuedAt))
    }
}
