package com.dozy.api.client

import com.dozy.api.contracts.v1.ApiErrorResponse

class DozyApiException(
    val status: Int,
    val error: ApiErrorResponse?,
) : IllegalStateException("Dozy API request failed (HTTP $status)")
