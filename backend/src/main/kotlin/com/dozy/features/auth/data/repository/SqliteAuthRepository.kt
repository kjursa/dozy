package com.dozy.features.auth.data.repository

import com.dozy.domain.id.EmailSessionId
import com.dozy.domain.id.OwnerId
import com.dozy.features.auth.data.database.AuthEmailCodes
import com.dozy.features.auth.data.database.AuthRefreshTokens
import com.dozy.features.auth.data.database.AuthUsers
import com.dozy.features.auth.domain.model.CodeVerification
import com.dozy.features.auth.domain.model.EmailChallenge
import com.dozy.features.auth.domain.model.RefreshCredential
import com.dozy.features.auth.domain.repository.AuthRepository
import com.dozy.infrastructure.database.SqliteDatabase
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greater
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update

class SqliteAuthRepository(
    private val database: SqliteDatabase,
) : AuthRepository {
    override suspend fun createAnonymous(
        ownerId: OwnerId,
        refresh: RefreshCredential,
    ) {
        database.write {
            insertOwner(ownerId, "anonymous+${ownerId.value}@invalid.dozy")
            insertRefreshToken(ownerId, refresh)
        }
    }

    override suspend fun storeChallenge(
        challenge: EmailChallenge,
        countSince: Long,
        limit: Int,
    ): Boolean =
        database.write {
            val recentCount =
                AuthEmailCodes
                    .selectAll()
                    .where {
                        (AuthEmailCodes.email eq challenge.email) and (AuthEmailCodes.createdAt greaterEq countSince)
                    }.count()
            if (recentCount >= limit) return@write false
            AuthEmailCodes.insert {
                it[sessionId] = challenge.sessionId.value
                it[email] = challenge.email
                it[code] = challenge.code
                it[createdAt] = challenge.createdAt
                it[expiresAt] = challenge.expiresAt
                it[attempts] = 0
                it[consumed] = false
            }
            true
        }

    override suspend fun verifyChallenge(
        sessionId: EmailSessionId,
        code: String,
        now: Long,
        maxAttempts: Int,
        newOwnerId: OwnerId,
        refresh: RefreshCredential,
    ): CodeVerification =
        database.write {
            val row =
                AuthEmailCodes.selectAll().where { AuthEmailCodes.sessionId eq sessionId.value }.singleOrNull()
                    ?: return@write CodeVerification.SessionExpired
            if (row[AuthEmailCodes.consumed] || row[AuthEmailCodes.expiresAt] <= now) return@write CodeVerification.SessionExpired
            val attempts = row[AuthEmailCodes.attempts]
            if (attempts >= maxAttempts) return@write CodeVerification.TooManyAttempts
            if (code != row[AuthEmailCodes.code]) {
                val nextAttempt = attempts + 1
                AuthEmailCodes.update({ AuthEmailCodes.sessionId eq sessionId.value }) { it[AuthEmailCodes.attempts] = nextAttempt }
                return@write if (nextAttempt >= maxAttempts) CodeVerification.TooManyAttempts else CodeVerification.InvalidCode
            }

            AuthEmailCodes.update({ AuthEmailCodes.sessionId eq sessionId.value }) { it[consumed] = true }
            val email = row[AuthEmailCodes.email]
            val ownerId =
                AuthUsers
                    .selectAll()
                    .where { AuthUsers.email eq email }
                    .singleOrNull()
                    ?.let { requireNotNull(OwnerId.parse(it[AuthUsers.ownerId])) }
                    ?: newOwnerId.also { insertOwner(it, email) }
            insertRefreshToken(ownerId, refresh)
            CodeVerification.Verified(ownerId)
        }

    override suspend fun rotateRefreshToken(
        ownerId: OwnerId,
        hashes: Set<String>,
        replacement: RefreshCredential,
        now: Long,
    ): Boolean =
        database.write {
            val row =
                AuthRefreshTokens
                    .selectAll()
                    .where {
                        (AuthRefreshTokens.tokenHash inList hashes) and
                            (AuthRefreshTokens.ownerId eq ownerId.value) and
                            (AuthRefreshTokens.revoked eq false) and
                            (AuthRefreshTokens.expiresAt greater now)
                    }.singleOrNull() ?: return@write false
            val hash = row[AuthRefreshTokens.tokenHash]
            AuthRefreshTokens.update({ AuthRefreshTokens.tokenHash eq hash }) { it[revoked] = true }
            insertRefreshToken(ownerId, replacement)
            true
        }

    override suspend fun revokeRefreshTokens(ownerId: OwnerId) {
        database.write {
            AuthRefreshTokens.update({ AuthRefreshTokens.ownerId eq ownerId.value }) { it[revoked] = true }
        }
    }

    private fun JdbcTransaction.insertOwner(
        id: OwnerId,
        address: String,
    ) {
        AuthUsers.insert {
            it[ownerId] = id.value
            it[email] = address
        }
    }

    private fun JdbcTransaction.insertRefreshToken(
        owner: OwnerId,
        refresh: RefreshCredential,
    ) {
        AuthRefreshTokens.insert {
            it[tokenHash] = refresh.hash
            it[ownerId] = owner.value
            it[expiresAt] = refresh.expiresAt
            it[revoked] = false
        }
    }
}
