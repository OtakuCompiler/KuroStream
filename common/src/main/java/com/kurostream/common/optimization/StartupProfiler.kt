package com.kurostream.common.optimization

import timber.log.Timber

class StartupProfiler {
    private val startTime = System.currentTimeMillis()
    private var processStart: Long = 0
    private var firstDraw: Long = 0
    private var fullyDrawn: Long = 0

    fun markProcessStart() {
        processStart = System.currentTimeMillis()
        Timber.d("Startup: process start marked")
    }

    fun markFirstDraw() {
        firstDraw = System.currentTimeMillis()
        val elapsed = firstDraw - processStart
        Timber.d("Startup: first draw in ${elapsed}ms")
    }

    fun markFullyDrawn() {
        fullyDrawn = System.currentTimeMillis()
        val elapsed = fullyDrawn - processStart
        Timber.d("Startup: fully drawn in ${elapsed}ms")
    }
}
