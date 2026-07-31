package com.kurostream.common.optimization

import android.os.Build
import android.os.SystemClock
import timber.log.Timber

class StartupProfiler {
    private val processStart: Long = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        android.os.Process.getStartUptimeMillis()
    } else {
        System.currentTimeMillis()
    }
    private var firstDraw: Long = 0
    private var fullyDrawn: Long = 0

    fun markFirstDraw() {
        firstDraw = SystemClock.uptimeMillis()
        Timber.d("Startup: first draw in ${firstDraw - processStart}ms")
    }

    fun markFullyDrawn() {
        fullyDrawn = SystemClock.uptimeMillis()
        Timber.d("Startup: fully drawn in ${fullyDrawn - processStart}ms")
    }
}
