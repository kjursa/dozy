package com.dozy.features.auth.application

import com.dozy.config.AuthSecretProvider
import com.dozy.domain.id.OwnerId
import io.jsonwebtoken.JwsHeader
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.Locator
import java.time.Clock
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneOffset
import java.util.Date

class AccessTokens(
    private val secrets: AuthSecretProvider,
    private val clock: Clock,
) {
    fun issue(ownerId: OwnerId): String {
        val now = clock.instant()
        val period = periodKey(now)
        return Jwts
            .builder()
            .header()
            .type("JWT")
            .keyId(period)
            .and()
            .issuer(ISSUER)
            .audience()
            .single(AUDIENCE)
            .subject(ownerId.value)
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plusSeconds(LIFETIME)))
            .signWith(secrets.jwtSecret(period), Jwts.SIG.HS256)
            .compact()
    }

    fun ownerId(token: String): OwnerId? {
        if (token.length > 2048) return null
        val now = clock.instant()
        return try {
            val jwt =
                Jwts
                    .parser()
                    .sig()
                    .clear()
                    .add(Jwts.SIG.HS256)
                    .and()
                    .zip()
                    .clear()
                    .and()
                    .requireIssuer(ISSUER)
                    .requireAudience(AUDIENCE)
                    .clock { Date.from(now) }
                    .keyLocator(
                        Locator { header ->
                            val period = (header as? JwsHeader)?.keyId
                            if (header.type != "JWT" || period == null ||
                                (period != periodKey(now) && period != periodKey(now.minusSeconds(LIFETIME)))
                            ) {
                                throw JwtException("Unsupported access token header")
                            }
                            secrets.jwtSecret(period)
                        },
                    ).build()
                    .parseSignedClaims(token)

            val issuedAt = jwt.payload.issuedAt?.toInstant() ?: return null
            val expiresAt = jwt.payload.expiration?.toInstant() ?: return null
            val owner = jwt.payload.subject ?: return null
            if (issuedAt > now || expiresAt <= now || expiresAt != issuedAt.plusSeconds(LIFETIME) ||
                jwt.header.keyId != periodKey(issuedAt)
            ) {
                return null
            }
            OwnerId.parse(owner)?.takeIf { it.value == owner }
        } catch (_: JwtException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    private fun periodKey(now: Instant): String = YearMonth.from(now.atZone(ZoneOffset.UTC)).toString()

    private companion object {
        const val LIFETIME = 20 * 60L
        const val ISSUER = "dozy"
        const val AUDIENCE = "dozy-mobile"
    }
}
