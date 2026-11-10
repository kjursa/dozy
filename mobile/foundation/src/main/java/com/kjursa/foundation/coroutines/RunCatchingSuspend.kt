package com.kjursa.foundation.coroutines

import kotlinx.coroutines.CancellationException

/** Captures ordinary exceptions while allowing cancellation and unrecoverable errors to propagate. */
suspend inline fun <T> runCatchingSuspend(block: () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (failure: Exception) {
        Result.failure(failure)
    }
