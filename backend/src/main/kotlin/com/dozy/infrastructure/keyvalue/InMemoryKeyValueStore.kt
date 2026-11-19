package com.dozy.infrastructure.keyvalue

import java.time.Clock
import java.time.Instant
import kotlin.time.Duration
import kotlin.time.toJavaDuration

class InMemoryKeyValueStore(
    private val clock: Clock,
) : KeyValueStore {
    private val entries = HashMap<String, Entry>()
    private var nextCleanup = Instant.MIN

    override suspend fun get(key: String): String? =
        synchronized(entries) {
            val entry = entries[key] ?: return@synchronized null
            if (entry.expiresAt > clock.instant()) return@synchronized entry.value
            entries.remove(key)
            null
        }

    override suspend fun put(
        key: String,
        value: String,
        ttl: Duration,
    ) {
        require(ttl.isFinite() && ttl.isPositive()) { "TTL must be positive and finite" }
        synchronized(entries) {
            val now = clock.instant()
            if (now >= nextCleanup) {
                entries.values.removeIf { it.expiresAt <= now }
                nextCleanup = now.plusSeconds(60)
            }
            entries[key] = Entry(value, now.plus(ttl.toJavaDuration()))
        }
    }

    override suspend fun delete(key: String) {
        synchronized(entries) { entries.remove(key) }
    }

    private data class Entry(
        val value: String,
        val expiresAt: Instant,
    )
}
