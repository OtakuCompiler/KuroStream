package com.kurostream.core.common.dispatcher

import android.content.Context
import android.os.PowerManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
actual class DefaultDispatcherProvider @Inject constructor(
    private val context: Context
) : DispatcherProvider {
    
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    
    actual override val main: CoroutineDispatcher = Dispatchers.Main
    actual override val io: CoroutineDispatcher = Dispatchers.IO
    actual override val default: CoroutineDispatcher = Dispatchers.Default
    actual override val unconfined: CoroutineDispatcher = Dispatchers.Unconfined

    // Thermal and battery-aware adaptive dispatchers
    actual override val computational: CoroutineDispatcher by lazy {
        val cores = getOptimalCoreCount()
        Dispatchers.Default.limitedParallelism(cores)
    }

    actual override val diskIO: CoroutineDispatcher by lazy {
        val cores = if (isPowerSaveMode()) 1 else Runtime.getRuntime().availableProcessors().coerceIn(2, 4)
        Dispatchers.IO.limitedParallelism(cores)
    }

    actual override val networkIO: CoroutineDispatcher by lazy {
        val cores = if (isPowerSaveMode()) 2 else Runtime.getRuntime().availableProcessors().coerceIn(2, 4)
        Dispatchers.IO.limitedParallelism(cores)
    }

    actual override val animation: CoroutineDispatcher = Dispatchers.Main

    // Thermal and battery-aware adaptive dispatchers
    val adaptiveIO: CoroutineDispatcher by lazy {
        val cores = getOptimalCoreCount()
        Dispatchers.IO.limitedParallelism(cores)
    }

    val cpuIntensive: CoroutineDispatcher by lazy {
        val cores = if (isPowerSaveMode()) {
            Runtime.getRuntime().availableProcessors().coerceIn(2, 4)
        } else {
            Runtime.getRuntime().availableProcessors().coerceIn(2, 6)
        }
        Dispatchers.Default.limitedParallelism(cores)
    }

    val databaseIO: CoroutineDispatcher by lazy {
        Dispatchers.IO.limitedParallelism(if (isPowerSaveMode()) 1 else 2)
    }

    val fileIO: CoroutineDispatcher by lazy {
        val cores = if (isPowerSaveMode()) 1 else Runtime.getRuntime().availableProcessors().coerceIn(2, 4)
        Dispatchers.IO.limitedParallelism(cores)
    }

    val imageProcessing: CoroutineDispatcher by lazy {
        Dispatchers.Default.limitedParallelism(if (isPowerSaveMode()) 1 else 2)
    }

    val backgroundSync: CoroutineDispatcher by lazy {
        Dispatchers.IO.limitedParallelism(1)
    }

    private fun isPowerSaveMode(): Boolean {
        return powerManager.isPowerSaveMode
    }

    private fun getOptimalCoreCount(): Int {
        val cores = Runtime.getRuntime().availableProcessors()
        return when {
            isPowerSaveMode() -> cores.coerceIn(2, 4)
            cores >= 8 -> 8
            cores >= 4 -> cores * 2
            else -> cores.coerceAtLeast(2)
        }
    }
}
