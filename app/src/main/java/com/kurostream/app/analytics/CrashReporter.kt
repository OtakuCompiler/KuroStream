package com.kurostream.app.analytics

import android.content.Context
import timber.log.Timber

class CrashReporter(context: Context) {
    init {
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Timber.e(throwable, "FATAL: Uncaught exception on ${thread.name}")
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    companion object {
        private var defaultHandler: Thread.UncaughtExceptionHandler? = null
        fun initialize(context: Context) {
            defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
            CrashReporter(context)
        }
    }
}
