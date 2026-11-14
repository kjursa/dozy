package com.dozy.api.client

sealed interface ApiResult<out T, out E> {
    data class Success<T>(val value: T) : ApiResult<T, Nothing>
    data class Failure<E>(val error: E) : ApiResult<Nothing, E>
}
