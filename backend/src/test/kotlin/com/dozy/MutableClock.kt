package com.dozy

import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.concurrent.atomic.AtomicReference
import kotlin.time.Duration
import kotlin.time.toJavaDuration

internal class MutableClock private constructor(
    private val time: AtomicReference<Instant>,
    private val zone: ZoneId,
) : Clock() {
    constructor() : this(AtomicReference(Instant.EPOCH), ZoneOffset.UTC)

    override fun getZone(): ZoneId = zone

    override fun withZone(zone: ZoneId): Clock = MutableClock(time, zone)

    override fun instant(): Instant = time.get()

    fun advance(duration: Duration) {
        time.updateAndGet { it.plus(duration.toJavaDuration()) }
    }
}
