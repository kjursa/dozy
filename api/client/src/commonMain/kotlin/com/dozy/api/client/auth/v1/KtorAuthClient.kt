package com.dozy.api.client.auth.v1

import com.dozy.api.client.DozyClientConfig
import com.dozy.api.client.businessFailure
import com.dozy.api.client.clientJson
import com.dozy.api.client.communicate
import com.dozy.api.contracts.ApiVersion
import com.dozy.api.contracts.v1.auth.AnonymousOwnerResponse
import com.dozy.api.contracts.v1.auth.AuthError
import com.dozy.api.contracts.v1.auth.AuthTokensResponse
import com.dozy.api.contracts.v1.auth.RefreshTokensRequest
import com.dozy.api.contracts.v1.auth.RefreshTokensResponse
import com.dozy.api.contracts.v1.auth.RequestEmailCodeRequest
import com.dozy.api.contracts.v1.auth.RequestEmailCodeResponse
import com.dozy.api.contracts.v1.auth.VerifyEmailCodeRequest
import io.ktor.client.HttpClient
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType

internal class KtorAuthClient(private val http: HttpClient, private val config: DozyClientConfig) {
    private fun endpoint(path: String) = config.endpoint(ApiVersion.V1, "auth/$path")

    suspend fun anonymous(): AnonymousResult = communicate(AnonymousResult::TechnicalError) {
        val response = http.post(endpoint("anonymous")) { expectSuccess = false }
        if (response.status.value == 200 || response.status.value == 201) clientJson.decodeFromString<AnonymousOwnerResponse>(response.bodyAsText()).let {
            AnonymousResult.Success(it.ownerId, it.accessToken, it.refreshToken)
        }
        else response.businessFailure(AuthError.serializer(), AnonymousResult::ApiError, AnonymousResult::TechnicalError)
    }

    suspend fun requestEmailCode(email: String): RequestEmailCodeResult = communicate(RequestEmailCodeResult::TechnicalError) {
        require(email.isNotBlank())
        val response = http.post(endpoint("email")) {
            expectSuccess = false
            contentType(ContentType.Application.Json)
            setBody(clientJson.encodeToString(RequestEmailCodeRequest(email)))
        }
        if (response.status.value == 200 || response.status.value == 202) RequestEmailCodeResult.Success(clientJson.decodeFromString<RequestEmailCodeResponse>(response.bodyAsText()).sessionId)
        else response.businessFailure(AuthError.serializer(), RequestEmailCodeResult::ApiError, RequestEmailCodeResult::TechnicalError)
    }

    suspend fun verifyEmailCode(sessionId: String, code: String): VerifyEmailCodeResult = communicate(VerifyEmailCodeResult::TechnicalError) {
        require(sessionId.isNotBlank() && code.matches(Regex("[0-9]{9}")))
        val response = http.post(endpoint("email/code")) {
            expectSuccess = false
            contentType(ContentType.Application.Json)
            setBody(clientJson.encodeToString(VerifyEmailCodeRequest(sessionId, code)))
        }
        if (response.status.value == 200) clientJson.decodeFromString<AuthTokensResponse>(response.bodyAsText()).let { VerifyEmailCodeResult.Success(it.ownerId, it.accessToken, it.refreshToken) }
        else response.businessFailure(AuthError.serializer(), VerifyEmailCodeResult::ApiError, VerifyEmailCodeResult::TechnicalError)
    }

    suspend fun refresh(ownerId: String, refreshToken: String): RefreshResult = communicate(RefreshResult::TechnicalError) {
        require(ownerId.isNotBlank() && refreshToken.isNotBlank())
        val response = http.post(endpoint("refresh")) {
            expectSuccess = false
            contentType(ContentType.Application.Json)
            setBody(clientJson.encodeToString(RefreshTokensRequest(ownerId, refreshToken)))
        }
        if (response.status.value == 200) clientJson.decodeFromString<RefreshTokensResponse>(response.bodyAsText()).let { RefreshResult.Success(it.accessToken, it.refreshToken) }
        else response.businessFailure(AuthError.serializer(), RefreshResult::ApiError, RefreshResult::TechnicalError)
    }

    suspend fun logout(accessToken: String): LogoutResult = communicate(LogoutResult::TechnicalError) {
        val response = http.post(endpoint("logout")) {
            expectSuccess = false
            bearerAuth(accessToken)
        }
        when (response.status.value) {
            in 200..299 -> LogoutResult.Success
            401 -> LogoutResult.ApiError(AuthError.InvalidAccessToken)
            else -> response.businessFailure(AuthError.serializer(), LogoutResult::ApiError, LogoutResult::TechnicalError)
        }
    }
}
