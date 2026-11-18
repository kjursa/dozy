package com.dozy.features.auth

import com.dozy.config.AuthSecretProvider
import com.dozy.config.DatabaseConfig
import com.dozy.features.auth.application.AccessTokens
import com.dozy.features.auth.application.AuthService
import com.dozy.features.auth.application.TokenResult
import com.dozy.features.auth.data.repository.SqliteAuthRepository
import com.dozy.features.auth.domain.model.RefreshCredential
import com.dozy.infrastructure.database.SqliteDatabase
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
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
            val token = "test-refresh-token"
            val legacyHash = "LfSDaU7trd8KkbLHb0Z5R-LZ3Fd2jQ73qqG6UDkgCMM"
            val owner = "019a0000-0000-7000-8000-000000000003"

            assertNotEquals(legacyHash, secrets.hashRefreshToken(token))

            SqliteDatabase(DatabaseConfig(directory.resolve("auth.db").toString(), 5000)).use { database ->
                val repository = SqliteAuthRepository(database)
                val auth = AuthService(repository, secrets, AccessTokens(secrets))
                val expiresAt = System.currentTimeMillis() + 60_000
                repository.createAnonymous(owner, RefreshCredential(legacyHash, expiresAt))

                val rotated = assertIs<TokenResult.Valid>(auth.refresh(owner, token))
                assertEquals(TokenResult.Invalid, auth.refresh(owner, token))
                assertTrue(
                    repository.rotateRefreshToken(
                        owner,
                        setOf(secrets.hashRefreshToken(rotated.refreshToken)),
                        RefreshCredential(secrets.hashRefreshToken("next-refresh"), expiresAt),
                        System.currentTimeMillis(),
                    ),
                )
            }
        }
}
