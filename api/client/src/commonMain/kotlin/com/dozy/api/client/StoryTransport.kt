package com.dozy.api.client

import com.dozy.api.contracts.v1.ApiErrorCode
import com.dozy.api.contracts.v1.ApiErrorResponse
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json

internal val storyJson = Json { ignoreUnknownKeys = true }
internal suspend fun HttpResponse.storyError(): ApiErrorResponse {
    if (status.value !in 400..599) throw InvalidResponseException(status.value)
    return storyJson.decodeFromString<ApiErrorResponse>(bodyAsText()).also {
        if (ApiErrorCode.fromValue(it.code) in setOf(ApiErrorCode.INVALID_REQUEST, ApiErrorCode.INVALID_INSTALLATION_ID)) {
            throw InvalidRequestException(status.value, it)
        }
    }
}
