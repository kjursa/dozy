package com.dozy.mobile.core.logging

import kotlinx.coroutines.flow.MutableStateFlow

/** App-wide logging. Install a sink once at startup to enable it. */
object DozyLog {
    enum class Level { DEBUG, INFO, WARN, ERROR }

    fun interface Sink {
        fun write(level: Level, tag: String, message: String, cause: Throwable?)
    }

    private val sink = MutableStateFlow<Sink?>(null)

    fun configure(newSink: Sink?) {
        sink.value = newSink
    }

    fun d(tag: String, message: () -> String) = write(Level.DEBUG, tag, null, message)

    fun i(tag: String, message: () -> String) = write(Level.INFO, tag, null, message)

    fun w(tag: String, cause: Throwable? = null, message: () -> String) = write(Level.WARN, tag, cause, message)

    fun e(tag: String, cause: Throwable? = null, message: () -> String) = write(Level.ERROR, tag, cause, message)

    private fun write(level: Level, tag: String, cause: Throwable?, message: () -> String) {
        sink.value?.write(level, tag, message(), cause)
    }
}
