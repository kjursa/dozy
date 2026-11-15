package com.dozy.api.client

sealed interface ClientError<out E> {
    data class ApiError<E>(val error: E) : ClientError<E>
    data class ServerError(val status: Int) : ClientError<Nothing>
    data class CommunicationError(val exception: Exception) : ClientError<Nothing>
}
