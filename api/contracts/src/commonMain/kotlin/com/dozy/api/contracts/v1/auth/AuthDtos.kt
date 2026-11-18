package com.dozy.api.contracts.v1.auth

import kotlinx.serialization.Serializable

@Serializable
data class AnonymousOwnerResponse(val ownerId: String, val accessToken: String, val refreshToken: String)

@Serializable
data class RequestEmailCodeRequest(val email: String)

@Serializable
data class RequestEmailCodeResponse(val sessionId: String)

@Serializable
data class VerifyEmailCodeRequest(val sessionId: String, val code: String)

@Serializable
data class AuthTokensResponse(val ownerId: String, val accessToken: String, val refreshToken: String)

@Serializable
data class RefreshTokensRequest(val ownerId: String, val refreshToken: String)

@Serializable
data class RefreshTokensResponse(val accessToken: String, val refreshToken: String)
