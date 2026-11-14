package com.dozy.api.client

import com.dozy.api.contracts.v1.ApiErrorResponse

class InvalidRequestException(val status: Int, val error: ApiErrorResponse) :
    IllegalStateException("Dozy API rejected the request structure (HTTP $status)")

class InvalidResponseException(val status: Int) :
    IllegalStateException("Dozy API returned an unexpected response (HTTP $status)")
