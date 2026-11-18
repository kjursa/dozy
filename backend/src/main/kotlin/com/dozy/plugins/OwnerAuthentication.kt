package com.dozy.plugins

import com.dozy.features.auth.application.AccessTokens
import com.dozy.features.installation.domain.model.InstallationId
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.bearer
import io.ktor.server.auth.principal

const val OWNER_AUTH = "owner"

private data class OwnerPrincipal(val id: String)

fun Application.configureOwnerAuthentication(tokens: AccessTokens) {
    install(Authentication) {
        bearer(OWNER_AUTH) {
            realm = "Dozy"
            authenticate { credential ->
                tokens.ownerId(credential.token)?.let(::OwnerPrincipal)
            }
        }
    }
}

fun ApplicationCall.ownerId(): String = checkNotNull(principal<OwnerPrincipal>()).id

// Existing repositories use InstallationId as the SQLite partition key.
fun ApplicationCall.ownerPartition(): InstallationId = InstallationId(ownerId())
