package com.dozy.infrastructure.keyvalue

import kotlin.time.Duration

interface KeyValueStore {
    suspend fun get(key: String): String?

    /** Replaces both the value and its expiry. TTL must be positive and finite. */
    suspend fun put(
        key: String,
        value: String,
        ttl: Duration,
    )

    suspend fun delete(key: String)
}
