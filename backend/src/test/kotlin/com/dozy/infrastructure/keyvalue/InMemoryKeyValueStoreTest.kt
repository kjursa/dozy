package com.dozy.infrastructure.keyvalue

import com.dozy.MutableClock
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class InMemoryKeyValueStoreTest {
    @Test
    fun expiresAtTheDeadlineAndReadsDoNotExtendTtl() =
        runBlocking {
            val clock = MutableClock()
            val store = InMemoryKeyValueStore(clock)
            assertNull(store.get("missing"))
            store.put("job", "queued", 1.seconds)
            clock.advance(999.milliseconds)
            assertEquals("queued", store.get("job"))
            clock.advance(1.milliseconds)
            assertNull(store.get("job"))
        }

    @Test
    fun replacingAnEntryResetsItsTtlAndDeletingOnlyAffectsItsKey() =
        runBlocking {
            val clock = MutableClock()
            val store = InMemoryKeyValueStore(clock)
            store.put("job", "queued", 2.seconds)
            store.put("other", "ready", 5.seconds)
            clock.advance(1.seconds)
            store.put("job", "running", 2.seconds)
            clock.advance(1.seconds)
            assertEquals("running", store.get("job"))
            clock.advance(1.seconds)
            assertNull(store.get("job"))
            store.delete("other")
            assertNull(store.get("other"))
            store.put("job", "ready", 2.seconds)
            store.delete("unknown")
            assertEquals("ready", store.get("job"))
        }

    @Test
    fun rejectsTtlThatIsInfiniteOrNonpositive() =
        runBlocking {
            val store = InMemoryKeyValueStore(MutableClock())
            for (ttl in listOf(Duration.ZERO, -1.seconds, Duration.INFINITE)) {
                assertFailsWith<IllegalArgumentException> { store.put("job", "ready", ttl) }
            }
        }
}
