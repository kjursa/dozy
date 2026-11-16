package com.dozy.api.client

import com.dozy.api.contracts.v1.ApiBusinessError
import com.dozy.api.contracts.v1.ApiErrorCode
import com.dozy.api.contracts.v1.ApiErrorResponse
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

internal val clientJson = Json { ignoreUnknownKeys = true; encodeDefaults = true }

internal suspend fun <E : ApiBusinessError, R> HttpResponse.businessFailure(serializer: KSerializer<E>, apiError: (E) -> R, technicalError: (TechnicalError) -> R): R {
    if (status.value in 500..599) return technicalError(TechnicalError.Server(status.value))
    if (status.value !in 400..499) throw InvalidResponseException(status.value)
    val error = clientJson.decodeFromString(serializer, bodyAsText())
    if (ApiErrorCode.fromValue(error.code) in setOf(ApiErrorCode.INVALID_REQUEST, ApiErrorCode.INVALID_INSTALLATION_ID)) {
        throw InvalidRequestException(status.value, ApiErrorResponse(error.code))
    }
    return apiError(error)
}

internal suspend fun <R> communicate(technicalError: (TechnicalError) -> R, block: suspend () -> R): R =
    try { block() }
    catch (failure: CancellationException) { throw failure }
    catch (failure: Exception) { technicalError(TechnicalError.Communication(failure)) }
