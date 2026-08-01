package com.kurostream.app.performance

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import timber.log.Timber

class AnrWatcher(private val thresholdMs: Long = 5000) : Thread() {
    private val mainHandler = Handler(Looper.getMainLooper())
    @Volatile private var lastTick = 0L

    init {
        name = "ANR-Watcher"
    }

    override fun run() {
        while (!isInterrupted) {
            lastTick = SystemClock.uptimeMillis()
            mainHandler.postAtFrontOfQueue { lastTick = SystemClock.uptimeMillis() }
            sleep(thresholdMs)
            if (SystemClock.uptimeMillis() - lastTick > thresholdMs) {
                Timber.w("ANR detected: main thread blocked for >${thresholdMs}ms")
            }
        }
    }
}
