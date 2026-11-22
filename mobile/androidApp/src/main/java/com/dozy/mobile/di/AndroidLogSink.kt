package com.dozy.mobile.di

import android.util.Log
import com.dozy.mobile.core.logging.DozyLog

internal object AndroidLogSink : DozyLog.Sink {
    override fun write(level: DozyLog.Level, tag: String, message: String, cause: Throwable?) {
        when (level) {
            DozyLog.Level.DEBUG -> Log.d(tag, message, cause)
            DozyLog.Level.INFO -> Log.i(tag, message, cause)
            DozyLog.Level.WARN -> Log.w(tag, message, cause)
            DozyLog.Level.ERROR -> Log.e(tag, message, cause)
        }
    }
}
