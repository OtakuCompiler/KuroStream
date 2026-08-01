package com.kurostream.common.memory

import android.app.ActivityManager
import android.content.Context
import android.os.Debug
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import coil.ImageLoader
import timber.log.Timber

class RamEnforcer(
    context: Context,
    private val targetMaxMb: Int = 100,
    private val criticalMaxMb: Int = 115,
    private val absoluteMaxMb: Int = 125,
) {
    private val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        ?: throw IllegalStateException("ActivityManager not available")
    private val context: Context = context
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val trimListeners = mutableListOf<suspend () -> Unit>()
    private val _pressureLevel = MutableStateFlow(PressureLevel.NOMINAL)
    val pressureLevel: StateFlow<PressureLevel> = _pressureLevel.asStateFlow()

    enum class PressureLevel { NOMINAL, ELEVATED, CRITICAL, EMERGENCY }

    fun startMonitoring() {
        scope.launch {
            while (isActive) {
                val memInfo = Debug.MemoryInfo()
                Debug.getMemoryInfo(memInfo)
                val pssMb = memInfo.totalPss / 1024

                _pressureLevel.value = when {
                    pssMb > absoluteMaxMb -> PressureLevel.EMERGENCY
                    pssMb > criticalMaxMb -> PressureLevel.CRITICAL
                    pssMb > targetMaxMb -> PressureLevel.ELEVATED
                    else -> PressureLevel.NOMINAL
                }

                when {
                    pssMb > absoluteMaxMb -> {
                        Timber.e("RAM DEATHLINE: ${pssMb}MB > ${absoluteMaxMb}MB — releasing all")
                        emergencyCleanup()
                    }
                    pssMb > criticalMaxMb -> {
                        Timber.w("RAM CRITICAL: ${pssMb}MB > ${criticalMaxMb}MB")
                        trimAllCaches()
                    }
                }

                delay(3000)
            }
        }
    }

    fun registerTrimListener(listener: suspend () -> Unit) {
        trimListeners.add(listener)
    }

    private fun trimAllCaches() {
        scope.launch {
            trimListeners.forEach {
                try { it() } catch (e: Exception) { Timber.w(e, "Trim listener failed") }
            }
            coil.ImageLoader(context).memoryCache?.clear()
        }
    }

    private fun emergencyCleanup() {
        scope.launch {
            trimAllCaches()
        }
    }

    fun stop() {
        scope.cancel()
    }
}
