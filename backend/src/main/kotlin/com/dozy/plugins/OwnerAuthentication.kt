package com.dozy.plugins

import com.dozy.domain.id.OwnerId
import com.dozy.features.auth.application.AccessTokens
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.bearer
import io.ktor.server.auth.principal

const val OWNER_AUTH = "owner"

private data class OwnerPrincipal(val id: OwnerId)

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

fun ApplicationCall.ownerId(): OwnerId = checkNotNull(principal<OwnerPrincipal>()).id
