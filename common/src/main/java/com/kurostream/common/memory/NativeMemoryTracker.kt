package com.kurostream.common.memory

import android.os.Debug
import timber.log.Timber
import timber.log.Timber
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Tracks native memory allocations and alerts on thresholds.
 * Integrates with AdaptiveMemoryGovernor for unified pressure reporting.
 * Target: Detect leaks, prevent OOM on TV devices.
 */
class NativeMemoryTracker(
    private val memoryGovernor: AdaptiveMemoryGovernor? = null,
    private val warningThresholdPercent: Float = 0.8f,
    private val criticalThresholdPercent: Float = 0.95f,
    private val reportingIntervalMs: Long = 30_000
) {
    private var trackingJob: Job? = null
    private var maxNativeHeap = 0L
    private var lastNativeHeap = 0L
    private var lastJavaHeap = 0L
    private var allocationCount = 0L
    
    // State flows for observation
    private val _nativeHeap = MutableStateFlow(0L)
    val nativeHeap: StateFlow<Long> = _nativeHeap.asStateFlow()
    
    private val _javaHeap = MutableStateFlow(0L)
    val javaHeap: StateFlow<Long> = _javaHeap.asStateFlow()
    
    private val _alertLevel = MutableStateFlow(AlertLevel.NORMAL)
    val alertLevel: StateFlow<AlertLevel> = _alertLevel.asStateFlow()
    
    private val _allocationRate = MutableStateFlow(0.0)
    val allocationRate: StateFlow<Double> = _allocationRate.asStateFlow()
    
    enum class AlertLevel {
        NORMAL, WARNING, CRITICAL
    }
    
    data class MemorySnapshot(
        val nativeHeap: Long,
        val javaHeap: Long,
        val maxNativeHeap: Long,
        val alertLevel: AlertLevel,
        val allocationRate: Double,
        val timestamp: Long = System.currentTimeMillis()
    )
    
    private val _latestSnapshot = MutableStateFlow<MemorySnapshot?>(null)
    val latestSnapshot: StateFlow<MemorySnapshot?> = _latestSnapshot.asStateFlow()
    
    /**
     * Starts periodic memory tracking.
     */
    fun startTracking(scope: CoroutineScope) {
        if (trackingJob?.isActive == true) return
        
        trackingJob = scope.launch(Dispatchers.IO) {
            while (isActive) {
                try {
                    collectAndReport()
                } catch (e: Exception) {
                    Log.w("NativeMemoryTracker", "Tracking error", e)
                }
                delay(reportingIntervalMs)
            }
        }
    }
    
    /**
     * Stops tracking.
     */
    fun stopTracking() {
        trackingJob?.cancel()
        trackingJob = null
    }
    
    /**
     * Collects memory stats and reports to governor.
     */
    private fun collectAndReport() {
        val nativeHeap = Debug.getNativeHeapAllocatedSize()
        val nativeFree = Debug.getNativeHeapFreeSize()
        val nativeMax = Debug.getNativeHeapSize()
        val javaHeap = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
        val javaMax = Runtime.getRuntime().maxMemory()
        
        _nativeHeap.value = nativeHeap
        _javaHeap.value = javaHeap
        
        // Update max
        if (nativeHeap > maxNativeHeap) {
            maxNativeHeap = nativeHeap
        }
        
        // Calculate allocation rate
        val now = System.currentTimeMillis()
        val timeDelta = (now - lastReportTime) / 1000.0 // seconds
        if (timeDelta > 0) {
            val heapDelta = nativeHeap - lastNativeHeap
            _allocationRate.value = if (heapDelta > 0) heapDelta / timeDelta else 0.0
        }
        lastNativeHeap = nativeHeap
        lastJavaHeap = javaHeap
        lastReportTime = now
        
        // Determine alert level
        val nativePercent = nativeHeap.toDouble() / nativeMax
        val alertLevel = when {
            nativePercent >= criticalThresholdPercent -> AlertLevel.CRITICAL
            nativePercent >= warningThresholdPercent -> AlertLevel.WARNING
            else -> AlertLevel.NORMAL
        }
        
        _alertLevel.value = alertLevel
        
        val snapshot = MemorySnapshot(
            nativeHeap = nativeHeap,
            javaHeap = javaHeap,
            maxNativeHeap = nativeMax,
            alertLevel = alertLevel,
            allocationRate = _allocationRate.value
        )
        _latestSnapshot.value = snapshot
        
        // Report to AdaptiveMemoryGovernor
        memoryGovernor?.let { governor ->
            val totalPressure = (nativePercent * 0.6 + (javaHeap.toDouble() / javaMax) * 0.4).toFloat()
            governor.updateExternalPressure(totalPressure.coerceIn(0f, 1f))
        }
        
        // Log alerts
        when (alertLevel) {
            AlertLevel.NORMAL -> { /* No alert needed */ }
            AlertLevel.WARNING -> Log.w("NativeMemoryTracker",
                "Native heap warning: ${formatBytes(nativeHeap)} / ${formatBytes(nativeMax)} (${(nativePercent * 100).toInt()}%)")
            AlertLevel.CRITICAL -> Log.e("NativeMemoryTracker",
                "Native heap CRITICAL: ${formatBytes(nativeHeap)} / ${formatBytes(nativeMax)} (${(nativePercent * 100).toInt()}%)")
        }
    }
    
    private var lastReportTime = System.currentTimeMillis()
    
    /**
     * Gets current native heap usage.
     */
    fun getCurrentNativeHeap(): Long = _nativeHeap.value
    
    /**
     * Gets current Java heap usage.
     */
    fun getCurrentJavaHeap(): Long = _javaHeap.value
    
    /**
     * Gets max native heap observed.
     */
    fun getMaxNativeHeap(): Long = maxNativeHeap
    
    /**
     * Gets current alert level.
     */
    fun getAlertLevel(): AlertLevel = _alertLevel.value
    
    /**
     * Gets current allocation rate (bytes/sec).
     */
    fun getAllocationRate(): Double = _allocationRate.value
    
    /**
     * Gets latest memory snapshot.
     */
    fun getLatestSnapshot(): MemorySnapshot? = _latestSnapshot.value
    
    /**
     * Dumps detailed allocation info (debug builds).
     */
    fun dumpAllocations(): String {
        val sb = StringBuilder()
        sb.append("=== Native Memory Dump ===\n")
        sb.append("Native Heap: ${formatBytes(_nativeHeap.value)}\n")
        sb.append("Native Max: ${formatBytes(Debug.getNativeHeapSize())}\n")
        sb.append("Native Free: ${formatBytes(Debug.getNativeHeapFreeSize())}\n")
        sb.append("Java Heap: ${formatBytes(_javaHeap.value)}\n")
        sb.append("Java Max: ${formatBytes(Runtime.getRuntime().maxMemory())}\n")
        sb.append("Max Observed: ${formatBytes(maxNativeHeap)}\n")
        sb.append("Allocation Rate: ${formatBytes(_allocationRate.value.toLong())}/sec\n")
        sb.append("Alert Level: ${_alertLevel.value}\n")
        return sb.toString()
    }
    
    /**
     * Checks if direct buffer leak detected.
     */
    fun checkDirectBufferLeak(): Boolean {
        // Simple heuristic: if native heap grows but Java heap stable
        val nativeGrowth = _nativeHeap.value - lastNativeHeap
        val javaGrowth = _javaHeap.value - lastJavaHeap
        return nativeGrowth > 1024 * 1024 && javaGrowth < 100 * 1024 // 1MB native growth, <100KB Java
    }
    
    companion object {
        private fun formatBytes(bytes: Long): String {
            return when {
                bytes >= 1024L * 1024 * 1024 -> String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024))
                bytes >= 1024L * 1024 -> String.format("%.2f MB", bytes / (1024.0 * 1024))
                bytes >= 1024L -> String.format("%.2f KB", bytes / 1024.0)
                else -> "$bytes B"
            }
        }
    }
}