package com.kurostream.launcher.firebase.messaging

import com.google.firebase.perf.FirebasePerformance
import com.google.firebase.perf.metrics.Trace
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PerformanceMonitor @Inject constructor() {

    private val firebasePerformance: FirebasePerformance = FirebasePerformance.getInstance()

    fun startTrace(name: String): Trace {
        val trace = firebasePerformance.newTrace(name)
        trace.start()
        return trace
    }

    fun stopTrace(trace: Trace) {
        trace.stop()
    }

    fun setPerformanceCollectionEnabled(enabled: Boolean) {
        firebasePerformance.isPerformanceCollectionEnabled = enabled
    }
}
