package com.dozy.api.contracts

import com.dozy.api.contracts.v1.ApiErrorCode
import com.dozy.api.contracts.v1.ApiErrorResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.serialization.json.Json

class ApiErrorCodeTest {
    @Test
    fun `known v1 error codes have stable wire values`() {
        val expected = mapOf(
            ApiErrorCode.INVALID_REQUEST to "INVALID_REQUEST",
            ApiErrorCode.VALIDATION_FAILED to "VALIDATION_FAILED",
            ApiErrorCode.INVALID_OWNER_ID to "INVALID_OWNER_ID",
            ApiErrorCode.FORBIDDEN to "FORBIDDEN",
            ApiErrorCode.CONFLICT to "CONFLICT",
            ApiErrorCode.UNAVAILABLE to "UNAVAILABLE",
            ApiErrorCode.JOB_NOT_FAILED to "JOB_NOT_FAILED",
            ApiErrorCode.JOB_NOT_FOUND to "JOB_NOT_FOUND",
            ApiErrorCode.IDEMPOTENCY_CONFLICT to "IDEMPOTENCY_CONFLICT",
            ApiErrorCode.ACTIVE_JOB_EXISTS to "ACTIVE_JOB_EXISTS",
        )
        expected.forEach { (code, value) ->
            assertEquals(value, code.value)
            assertEquals(code, ApiErrorCode.fromValue(value))
        }
    }

    @Test
    fun `new or misspelled wire codes remain readable without a known enum value`() {
        for (code in listOf("NEW_RULE", "FORBID", "forbidden")) {
            val response = Json.decodeFromString<ApiErrorResponse>("""{"code":"$code"}""")
            assertEquals(code, response.code)
            assertNull(ApiErrorCode.fromValue(response.code))
        }
    }
}
