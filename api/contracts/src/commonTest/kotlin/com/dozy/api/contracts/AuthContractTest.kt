package com.dozy.api.contracts

import com.dozy.api.contracts.v1.auth.AuthTokensResponse
import com.dozy.api.contracts.v1.auth.RefreshTokensRequest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class AuthContractTest {
    @Test
    fun authWireModelsUseExpectedJsonFields() {
        val json = Json { encodeDefaults = true }
        assertEquals("""{"ownerId":"owner","accessToken":"access","refreshToken":"refresh"}""", json.encodeToString(AuthTokensResponse("owner", "access", "refresh")))
        assertEquals("""{"ownerId":"owner","refreshToken":"refresh"}""", json.encodeToString(RefreshTokensRequest("owner", "refresh")))
    }
}
