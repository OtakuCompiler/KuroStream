package com.kurostream.common.memory

import android.app.ActivityManager
import android.content.Context
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber

class RamEnforcer(
    context: Context,
    private val targetMaxMb: Int = 110,
    private val criticalMaxMb: Int = 125,
) {
    private val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _pressureLevel = MutableStateFlow(PressureLevel.NOMINAL)
    val pressureLevel: StateFlow<PressureLevel> = _pressureLevel.asStateFlow()

    enum class PressureLevel { NOMINAL, ELEVATED, CRITICAL, EMERGENCY }

    fun startMonitoring() {
        scope.launch {
            while (isActive) {
                val memInfo = ActivityManager.MemoryInfo()
                am.getMemoryInfo(memInfo)
                val totalMb = memInfo.totalMem / (1024 * 1024)

                _pressureLevel.value = when {
                    totalMb > criticalMaxMb -> PressureLevel.EMERGENCY
                    totalMb > targetMaxMb -> PressureLevel.CRITICAL
                    totalMb > targetMaxMb * 0.85 -> PressureLevel.ELEVATED
                    else -> PressureLevel.NOMINAL
                }

                if (totalMb > criticalMaxMb) {
                    Timber.w("RAM EMERGENCY: ${totalMb}MB > ${criticalMaxMb}MB")
                    emergencyCleanup()
                }

                delay(2000)
            }
        }
    }

    private fun emergencyCleanup() {
        System.gc()
        System.runFinalization()
    }

    fun stop() {
        scope.cancel()
    }
}
