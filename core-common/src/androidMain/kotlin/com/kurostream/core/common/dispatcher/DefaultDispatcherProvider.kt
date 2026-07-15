package com.kurostream.core.common.dispatcher

import android.content.Context
import android.os.PowerManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultDispatcherProvider : DispatcherProvider {
    
    @Inject
    lateinit var context: Context
    
    private val powerManager by lazy {
        context.getSystemService(Context.POWER_SERVICE) as PowerManager
    }
    
    override val main: CoroutineDispatcher = Dispatchers.Main
    override val io: CoroutineDispatcher = Dispatchers.IO
    override val default: CoroutineDispatcher = Dispatchers.Default
    override val unconfined: CoroutineDispatcher = Dispatchers.Unconfined

    // Thermal and battery-aware adaptive dispatchers
    override val computational: CoroutineDispatcher by lazy {
        val cores = getOptimalCoreCount()
        Dispatchers.Default.limitedParallelism(cores)
    }

    override val diskIO: CoroutineDispatcher by lazy {
        val cores = if (isPowerSaveMode()) 1 else Runtime.getRuntime().availableProcessors().coerceIn(2, 4)
        Dispatchers.IO.limitedParallelism(cores)
    }

    override val networkIO: CoroutineDispatcher by lazy {
        val cores = if (isPowerSaveMode()) 2 else Runtime.getRuntime().availableProcessors().coerceIn(2, 4)
        Dispatchers.IO.limitedParallelism(cores)
    }

    override val animation: CoroutineDispatcher = Dispatchers.Main
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
