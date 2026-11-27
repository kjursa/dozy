package com.dozy.features.auth.api.v1

import com.dozy.api.contracts.v1.auth.AnonymousOwnerResponse
import com.dozy.api.contracts.v1.auth.AuthError
import com.dozy.api.contracts.v1.auth.AuthTokensResponse
import com.dozy.api.contracts.v1.auth.RefreshTokensRequest
import com.dozy.api.contracts.v1.auth.RefreshTokensResponse
import com.dozy.api.contracts.v1.auth.RequestEmailCodeRequest
import com.dozy.api.contracts.v1.auth.RequestEmailCodeResponse
import com.dozy.api.contracts.v1.auth.VerifyEmailCodeRequest
import com.dozy.domain.id.EmailSessionId
import com.dozy.domain.id.OwnerId
import com.dozy.domain.id.RefreshToken
import com.dozy.features.auth.application.AuthService
import com.dozy.features.auth.application.EmailRequest
import com.dozy.features.auth.application.TokenResult
import com.dozy.features.auth.application.VerifyResult
import com.dozy.plugins.BusinessApiException
import com.dozy.plugins.OWNER_AUTH
import com.dozy.plugins.ownerId
import com.dozy.plugins.respondApiError
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.plugins.di.dependencies
import io.ktor.server.plugins.di.resolve
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post

fun Route.authV1Routes() {
    post("/auth/anonymous") {
        val service = call.application.dependencies.resolve<AuthService>()
        val tokens = service.anonymous()
        call.respond(HttpStatusCode.Created, AnonymousOwnerResponse(tokens.ownerId.value, tokens.accessToken, tokens.refreshToken.value))
    }
    post("/auth/email") {
        val service = call.application.dependencies.resolve<AuthService>()
        when (val result = service.requestCode(call.receive<RequestEmailCodeRequest>().email)) {
            is EmailRequest.Accepted -> call.respond(HttpStatusCode.Accepted, RequestEmailCodeResponse(result.sessionId.value))
            EmailRequest.InvalidEmail -> call.respondApiError(HttpStatusCode.UnprocessableEntity, AuthError.InvalidEmail)
            EmailRequest.RateLimited -> call.respondApiError(HttpStatusCode.TooManyRequests, AuthError.RateLimited)
        }
    }
    post("/auth/email/code") {
        val request = call.receive<VerifyEmailCodeRequest>()
        val service = call.application.dependencies.resolve<AuthService>()
        val sessionId =
            EmailSessionId.parse(request.sessionId)
                ?: throw BusinessApiException(HttpStatusCode.Unauthorized, AuthError.SessionExpired)
        when (val result = service.verify(sessionId, request.code)) {
            is VerifyResult.Success ->
                result.tokens.let {
                    call.respond(HttpStatusCode.OK, AuthTokensResponse(it.ownerId.value, it.accessToken, it.refreshToken.value))
                }
            VerifyResult.InvalidCode -> call.respondApiError(HttpStatusCode.Unauthorized, AuthError.InvalidCode)
            VerifyResult.TooManyAttempts -> call.respondApiError(HttpStatusCode.TooManyRequests, AuthError.TooManyAttempts)
            VerifyResult.SessionExpired -> call.respondApiError(HttpStatusCode.Unauthorized, AuthError.SessionExpired)
        }
    }
    post("/auth/refresh") {
        val request = call.receive<RefreshTokensRequest>()
        val service = call.application.dependencies.resolve<AuthService>()
        val ownerId = OwnerId.parse(request.ownerId)
        val refreshToken = RefreshToken.parse(request.refreshToken)
        if (ownerId == null || refreshToken == null) {
            throw BusinessApiException(HttpStatusCode.Unauthorized, AuthError.InvalidRefreshToken)
        }
        when (val result = service.refresh(ownerId, refreshToken)) {
            is TokenResult.Valid -> call.respond(HttpStatusCode.OK, RefreshTokensResponse(result.accessToken, result.refreshToken.value))
            TokenResult.Invalid -> call.respondApiError(HttpStatusCode.Unauthorized, AuthError.InvalidRefreshToken)
        }
    }
    authenticate(OWNER_AUTH) {
        post("/auth/logout") {
            val service = call.application.dependencies.resolve<AuthService>()
            service.logout(call.ownerId())
            call.respond(HttpStatusCode.NoContent)
        }
    }
}
