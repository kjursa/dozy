package com.dozy.features.auth.data.database

import org.jetbrains.exposed.v1.core.Table

internal object AuthUsers : Table("auth_users") {
    val ownerId = text("owner_id")
    val email = text("email")
    override val primaryKey = PrimaryKey(ownerId)
}

internal object AuthEmailCodes : Table("auth_email_codes") {
    val sessionId = text("session_id")
    val email = text("email")
    val code = text("code")
    val createdAt = long("created_at")
    val expiresAt = long("expires_at")
    val attempts = integer("attempts")
    val consumed = bool("consumed")
    override val primaryKey = PrimaryKey(sessionId)
}

internal object AuthRefreshTokens : Table("auth_refresh_tokens") {
    val tokenHash = text("token_hash")
    val ownerId = text("owner_id")
    val expiresAt = long("expires_at")
    val revoked = bool("revoked")
    override val primaryKey = PrimaryKey(tokenHash)
}
