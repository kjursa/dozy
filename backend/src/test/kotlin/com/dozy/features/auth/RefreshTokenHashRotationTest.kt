package com.dozy.features.auth

import com.dozy.config.AuthSecretProvider
import com.dozy.config.DatabaseConfig
import com.dozy.domain.id.OwnerId
import com.dozy.domain.id.RefreshToken
import com.dozy.features.auth.application.AccessTokens
import com.dozy.features.auth.application.AuthService
import com.dozy.features.auth.application.TokenResult
import com.dozy.features.auth.data.repository.SqliteAuthRepository
import com.dozy.features.auth.domain.model.RefreshCredential
import com.dozy.infrastructure.database.SqliteDatabase
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class RefreshTokenHashRotationTest {
    @TempDir
    lateinit var directory: Path

    @Test
    fun rotatesLegacyRefreshTokenIntoTheNewHashFormat() =
        runBlocking {
            val secrets = AuthSecretProvider("test-signing-key-with-at-least-32-characters")
            val token = requireNotNull(RefreshToken.parse("ref-019a0000-0000-4000-8000-000000000003"))
            val legacyHash = "c2Nhx9UentAO_Q5HAaA2SkF-8_bLqrMcpL9VKJzNei0"
            val owner = OwnerId.generate()

            assertNotEquals(legacyHash, secrets.hashRefreshToken(token))

            SqliteDatabase(DatabaseConfig(directory.resolve("auth.db").toString(), 5000)).use { database ->
                val repository = SqliteAuthRepository(database)
                val clock = Clock.fixed(Instant.parse("2026-11-30T23:55:00Z"), ZoneOffset.UTC)
                val auth = AuthService(repository, secrets, AccessTokens(secrets, clock), clock)
                val expiresAt = clock.millis() + 60_000
                repository.createAnonymous(owner, RefreshCredential(legacyHash, expiresAt))

                val rotated = assertIs<TokenResult.Valid>(auth.refresh(owner, token))
                assertEquals(TokenResult.Invalid, auth.refresh(owner, token))
                assertTrue(
                    repository.rotateRefreshToken(
                        owner,
                        setOf(secrets.hashRefreshToken(rotated.refreshToken)),
                        RefreshCredential(secrets.hashRefreshToken(RefreshToken.generate()), expiresAt),
                        clock.millis(),
                    ),
                )
            }
        }
}
