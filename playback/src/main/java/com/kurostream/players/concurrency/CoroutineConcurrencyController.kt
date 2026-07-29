package com.kurostream.players.concurrency

import android.os.Handler
import android.os.Looper
import androidx.annotation.VisibleForTesting
import com.kurostream.players.memory.AdaptiveMemoryManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Dynamic coroutine concurrency controller that adjusts parallelism based on memory pressure.
 * Integrates with AdaptiveMemoryManager to automatically scale concurrency up/down.
 *
 * Features:
 * - Semaphore-based limiting per dispatcher (IO, Default, Custom)
 * - Dynamic scaling based on memory pressure (0.0-1.0)
 * - Auto-scale down when pressure > 0.7, scale up when pressure < 0.3
 * - Per-dispatcher configuration with minimum/maximum bounds
 * - Integration with AdaptiveMemoryManager
 * - Thread-safe with proper synchronization
 */
@Singleton
class CoroutineConcurrencyController @Inject constructor(
    private val memoryManager: AdaptiveMemoryManager,
) {

    private val TAG = "CoroutineConcurrencyController"

    // Configuration constants
    private const val DEFAULT_MAX_CONCURRENCY = 8
    private const val DEFAULT_MIN_CONCURRENCY = 1
    private const val PRESSURE_SCALE_DOWN_THRESHOLD = 0.7f
    private const val PRESSURE_SCALE_UP_THRESHOLD = 0.3f
    private const val PRESSURE_CRITICAL_THRESHOLD = 0.9f
    private const val ADJUSTMENT_INTERVAL_MS = 5_000
    private const val MIN_ADJUSTMENT_INTERVAL_MS = 1_000

    // Dispatcher configurations
    private val dispatcherConfigs = mutableMapOf<CoroutineDispatcher, DispatcherConfig>()
    private val semaphores = mutableMapOf<CoroutineDispatcher, Semaphore>()

    // State flows for reactive UI/monitoring
    private val _currentConcurrency = MutableStateFlow<Map<CoroutineDispatcher, Int>>(emptyMap())
    val currentConcurrency: StateFlow<Map<CoroutineDispatcher, Int>> = _currentConcurrency.asStateFlow()

    private val _memoryPressure = MutableStateFlow(0.0f)
    val memoryPressure: StateFlow<Float> = _memoryPressure.asStateFlow()

    private val _isScalingDown = MutableStateFlow(false)
    val isScalingDown: StateFlow<Boolean> = _isScalingDown.asStateFlow()

    // Statistics
    private val totalAcquires = AtomicLong(0)
    private val totalReleases = AtomicLong(0)
    private val totalTimeouts = AtomicLong(0)
    private val currentWaiters = AtomicInteger(0)
    private val lastAdjustmentTime = AtomicLong(0)

    // Background monitoring
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val handler = Handler(Looper.getMainLooper())
    private var monitoringJob: kotlinx.coroutines.Job? = null

    init {
        // Initialize default dispatchers
        initializeDispatcher(Dispatchers.IO, maxConcurrency = 8, minConcurrency = 2)
        initializeDispatcher(Dispatchers.Default, maxConcurrency = 6, minConcurrency = 2)
        initializeDispatcher(Dispatchers.Unconfined, maxConcurrency = 4, minConcurrency = 1)

        // Start memory pressure monitoring
        startMonitoring()
        Timber.d("CoroutineConcurrencyController initialized")
    }

    /**
     * Configures concurrency limits for a specific dispatcher.
     */
    fun configureDispatcher(
        dispatcher: CoroutineDispatcher,
        maxConcurrency: Int = DEFAULT_MAX_CONCURRENCY,
        minConcurrency: Int = DEFAULT_MIN_CONCURRENCY,
        pressureScaleDownThreshold: Float = PRESSURE_SCALE_DOWN_THRESHOLD,
        pressureScaleUpThreshold: Float = PRESSURE_SCALE_UP_THRESHOLD,
    ) {
        val config = DispatcherConfig(
            dispatcher = dispatcher,
            maxConcurrency = maxConcurrency.coerceIn(1, 64),
            minConcurrency = minConcurrency.coerceIn(1, maxConcurrency),
            pressureScaleDownThreshold = pressureScaleDownThreshold.coerceIn(0.1f, 0.95f),
            pressureScaleUpThreshold = pressureScaleUpThreshold.coerceIn(0.05f, pressureScaleDownThreshold - 0.05f),
        )
        dispatcherConfigs[dispatcher] = config

        // Recreate semaphore with new limits
        val currentPermits = semaphores[dispatcher]?.availablePermits() ?: config.maxConcurrency
        val newPermits = currentPermits.coerceIn(config.minConcurrency, config.maxConcurrency)
        semaphores[dispatcher] = Semaphore(newPermits)

        updateConcurrencyFlow()
        Timber.d("Configured ${dispatcher.name}: max=${config.maxConcurrency}, min=${config.minConcurrency}, current=$newPermits")
    }

    /**
     * Initializes a dispatcher with default configuration.
     */
    private fun initializeDispatcher(
        dispatcher: CoroutineDispatcher,
        maxConcurrency: Int,
        minConcurrency: Int,
    ) {
        val config = DispatcherConfig(
            dispatcher = dispatcher,
            maxConcurrency = maxConcurrency,
            minConcurrency = minConcurrency,
        )
        dispatcherConfigs[dispatcher] = config
        semaphores[dispatcher] = Semaphore(maxConcurrency)
    }

    /**
     * Acquires a permit for the given dispatcher, suspending until available.
     * Returns a [ConcurrencyPermit] that must be released (via close() or use block).
     */
    suspend fun acquirePermit(dispatcher: CoroutineDispatcher = Dispatchers.Default): ConcurrencyPermit {
        val semaphore = semaphores[dispatcher] ?: Semaphore(DEFAULT_MAX_CONCURRENCY).also { semaphores[dispatcher] = it }
        val config = dispatcherConfigs[dispatcher] ?: DispatcherConfig(dispatcher, DEFAULT_MAX_CONCURRENCY, DEFAULT_MIN_CONCURRENCY)

        currentWaiters.incrementAndGet()
        try {
            // Try to acquire with timeout based on pressure
            val timeoutMs = calculateAcquireTimeout(config)
            val acquired = if (timeoutMs > 0) {
                withContext(dispatcher) {
                    semaphore.tryAcquire(timeoutMs, TimeUnit.MILLISECONDS)
                }
            } else {
                semaphore.tryAcquire()
            }

            if (!acquired) {
                totalTimeouts.incrementAndGet()
                throw ConcurrencyTimeoutException("Failed to acquire permit for ${dispatcher.name} within ${timeoutMs}ms")
            }

            totalAcquires.incrementAndGet()
            return ConcurrencyPermit(semaphore, dispatcher, this)
        } finally {
            currentWaiters.decrementAndGet()
        }
    }

    /**
     * Acquires a permit for the given dispatcher with a custom timeout.
     */
    suspend fun acquirePermit(
        dispatcher: CoroutineDispatcher,
        timeoutMs: Long,
        timeUnit: TimeUnit = TimeUnit.MILLISECONDS
    ): ConcurrencyPermit {
        val semaphore = semaphores[dispatcher] ?: Semaphore(DEFAULT_MAX_CONCURRENCY).also { semaphores[dispatcher] = it }

        currentWaiters.incrementAndGet()
        try {
            val acquired = withContext(dispatcher) {
                semaphore.tryAcquire(timeoutMs, timeUnit)
            }

            if (!acquired) {
                totalTimeouts.incrementAndGet()
                throw ConcurrencyTimeoutException("Failed to acquire permit for ${dispatcher.name} within ${timeUnit.toMillis(timeoutMs)}ms")
            }

            totalAcquires.incrementAndGet()
            return ConcurrencyPermit(semaphore, dispatcher, this)
        } finally {
            currentWaiters.decrementAndGet()
        }
    }

    /**
     * Executes a block with a permit for the given dispatcher.
     * Automatically releases the permit when the block completes.
     */
    suspend fun <T> withPermit(dispatcher: CoroutineDispatcher = Dispatchers.Default, block: suspend () -> T): T {
        val permit = acquirePermit(dispatcher)
        try {
            return block()
        } finally {
            permit.release()
        }
    }

    /**
     * Executes a block with a permit for the given dispatcher and timeout.
     */
    suspend fun <T> withPermit(
        dispatcher: CoroutineDispatcher,
        timeoutMs: Long,
        timeUnit: TimeUnit = TimeUnit.MILLISECONDS,
        block: suspend () -> T
    ): T {
        val permit = acquirePermit(dispatcher, timeoutMs, timeUnit)
        try {
            return block()
        } finally {
            permit.release()
        }
    }

    /**
     * Calculates acquire timeout based on current memory pressure.
     * Higher pressure = shorter timeout to fail fast.
     */
    private fun calculateAcquireTimeout(config: DispatcherConfig): Long {
        val pressure = _memoryPressure.value
        return when {
            pressure >= PRESSURE_CRITICAL_THRESHOLD -> 100 // Very short timeout
            pressure >= config.pressureScaleDownThreshold -> 500
            pressure >= 0.5f -> 2000
            else -> 5000
        }
    }

    /**
     * Starts monitoring memory pressure and adjusting concurrency.
     */
    private fun startMonitoring() {
        // Observe memory manager's pressure level
        scope.launch {
            memoryManager.memoryState.collect { state ->
                // Calculate pressure from available memory vs total
                val pressure = if (state.availableMemoryMb > 0) {
                    1f - (state.availableMemoryMb.toFloat() / state.memoryClass.toFloat()).coerceIn(0f, 1f)
                } else {
                    0f
                }
                _memoryPressure.value = pressure.coerceIn(0f, 1f)
                adjustConcurrencyForPressure(_memoryPressure.value)
            }
        }

        // Periodic adjustment as fallback
        monitoringJob = scope.launch {
            while (true) {
                kotlinx.coroutines.delay(ADJUSTMENT_INTERVAL_MS)
                val pressure = _memoryPressure.value
                adjustConcurrencyForPressure(pressure)
            }
        }
    }

    /**
     * Adjusts concurrency limits based on memory pressure.
     * Pressure > 0.7: reduce to 25% of max
     * Pressure > 0.5: reduce to 50% of max
     * Pressure < 0.3: restore to 100%
     */
    private fun adjustConcurrencyForPressure(pressure: Float) {
        val now = System.currentTimeMillis()
        val lastAdjustment = lastAdjustmentTime.getAndSet(now)

        // Rate limit adjustments
        if (now - lastAdjustment < MIN_ADJUSTMENT_INTERVAL_MS) {
            return
        }

        var anyChanged = false
        var scalingDown = false

        dispatcherConfigs.forEach { (dispatcher, config) ->
            val semaphore = semaphores[dispatcher] ?: return@forEach
            val currentPermits = config.maxConcurrency - semaphore.availablePermits()

            val targetPermits = when {
                pressure >= PRESSURE_CRITICAL_THRESHOLD -> config.minConcurrency
                pressure >= config.pressureScaleDownThreshold -> {
                    scalingDown = true
                    // Linear scale down from max to min between 0.7 and 1.0
                    val range = config.maxConcurrency - config.minConcurrency
                    val pressureRange = 1.0f - config.pressureScaleDownThreshold
                    val pressurePosition = (pressure - config.pressureScaleDownThreshold) / pressureRange
                    (config.maxConcurrency - (range * pressurePosition).toInt()).coerceIn(config.minConcurrency, config.maxConcurrency)
                }
                pressure <= config.pressureScaleUpThreshold -> config.maxConcurrency
                else -> {
                    // Gradual scale between scaleUp and scaleDown thresholds
                    val range = config.maxConcurrency - config.minConcurrency
                    val pressureRange = config.pressureScaleDownThreshold - config.pressureScaleUpThreshold
                    val pressurePosition = (pressure - config.pressureScaleUpThreshold) / pressureRange
                    (config.minConcurrency + (range * (1 - pressurePosition)).toInt()).coerceIn(config.minConcurrency, config.maxConcurrency)
                }
            }

            if (targetPermits != currentPermits) {
                adjustSemaphore(semaphore, currentPermits, targetPermits)
                anyChanged = true
                Timber.d("Adjusted ${dispatcher.name} concurrency: $currentPermits -> $targetPermits (pressure=${String.format("%.2f", pressure)})")
            }
        }

        _isScalingDown.value = scalingDown

        if (anyChanged) {
            updateConcurrencyFlow()
        }
    }

    /**
     * Adjusts semaphore permits to target count.
     */
    private fun adjustSemaphore(semaphore: Semaphore, current: Int, target: Int) {
        if (target > current) {
            // Release additional permits
            repeat(target - current) { semaphore.release() }
        } else if (target < current) {
            // Try to acquire permits to reduce (non-blocking)
            repeat(current - target) {
                if (!semaphore.tryAcquire()) {
                    // Could not reduce further, permits are in use
                    break
                }
            }
        }
    }

    /**
     * Updates the concurrency flow for observers.
     */
    private fun updateConcurrencyFlow() {
        val snapshot = semaphores.mapValues { (dispatcher, semaphore) ->
            val config = dispatcherConfigs[dispatcher] ?: DispatcherConfig(dispatcher, DEFAULT_MAX_CONCURRENCY, DEFAULT_MIN_CONCURRENCY)
            val inUse = config.maxConcurrency - semaphore.availablePermits()
            inUse.coerceAtLeast(0)
        }
        _currentConcurrency.value = snapshot
    }

    /**
     * Gets current concurrency (in-use permits) for a dispatcher.
     */
    fun getAvailablePermits(dispatcher: CoroutineDispatcher): Int {
        val semaphore = semaphores[dispatcher] ?: return 0
        return semaphore.availablePermits()
    }

    /**
     * Gets current in-use concurrency for a dispatcher.
     */
    fun getCurrentConcurrency(dispatcher: CoroutineDispatcher): Int {
        val semaphore = semaphores[dispatcher] ?: return 0
        val config = dispatcherConfigs[dispatcher] ?: return 0
        return (config.maxConcurrency - semaphore.availablePermits()).coerceAtLeast(0)
    }

    /**
     * Gets maximum concurrency for a dispatcher.
     */
    fun getMaxConcurrency(dispatcher: CoroutineDispatcher): Int {
        return dispatcherConfigs[dispatcher]?.maxConcurrency ?: DEFAULT_MAX_CONCURRENCY
    }

    /**
     * Gets minimum concurrency for a dispatcher.
     */
    fun getMinConcurrency(dispatcher: CoroutineDispatcher): Int {
        return dispatcherConfigs[dispatcher]?.minConcurrency ?: DEFAULT_MIN_CONCURRENCY
    }

    /**
     * Sets maximum concurrency for a dispatcher.
     */
    fun setMaxConcurrency(dispatcher: CoroutineDispatcher, max: Int) {
        val config = dispatcherConfigs[dispatcher] ?: DispatcherConfig(dispatcher, max, DEFAULT_MIN_CONCURRENCY)
        val newConfig = config.copy(maxConcurrency = max.coerceIn(1, 64))
        dispatcherConfigs[dispatcher] = newConfig

        // Adjust semaphore if needed
        val semaphore = semaphores[dispatcher] ?: Semaphore(newConfig.maxConcurrency).also { semaphores[dispatcher] = it }
        val currentInUse = newConfig.maxConcurrency - semaphore.availablePermits()
        if (currentInUse > newConfig.maxConcurrency) {
            // Too many in use, can't reduce immediately
            Timber.w("Cannot reduce max concurrency for ${dispatcher.name}: $currentInUse in use, max=$max")
        }
        updateConcurrencyFlow()
    }

    /**
     * Forces a concurrency adjustment (e.g., after configuration change).
     */
    fun forceAdjustment() {
        lastAdjustmentTime.set(0)
        adjustConcurrencyForPressure(_memoryPressure.value)
    }

    /**
     * Resets all dispatchers to their maximum concurrency.
     */
    fun resetToMax() {
        dispatcherConfigs.forEach { (dispatcher, config) ->
            val semaphore = semaphores[dispatcher] ?: return@forEach
            val currentInUse = config.maxConcurrency - semaphore.availablePermits()
            if (currentInUse < config.maxConcurrency) {
                repeat(config.maxConcurrency - currentInUse) { semaphore.release() }
            }
        }
        updateConcurrencyFlow()
        Timber.d("Reset all dispatchers to max concurrency")
    }

    /**
     * Gets current statistics.
     */
    fun getStats(): ConcurrencyStats {
        return ConcurrencyStats(
            totalAcquires = totalAcquires.get(),
            totalReleases = totalReleases.get(),
            totalTimeouts = totalTimeouts.get(),
            currentWaiters = currentWaiters.get(),
            currentConcurrency = _currentConcurrency.value.toMap(),
            maxConcurrency = dispatcherConfigs.mapValues { (d, c) -> c.maxConcurrency },
            minConcurrency = dispatcherConfigs.mapValues { (d, c) -> c.minConcurrency },
            memoryPressure = _memoryPressure.value,
            isScalingDown = _isScalingDown.value,
        )
    }

    /**
     * Shuts down the controller.
     */
    fun shutdown() {
        monitoringJob?.cancel()
        scope.cancel()
        handler.removeCallbacksAndMessages(null)
        // Release all permits
        semaphores.values.forEach { semaphore ->
            while (semaphore.tryAcquire()) { /* drain */ }
        }
        Timber.d("CoroutineConcurrencyController shutdown complete")
    }

    /**
     * Configuration for a single dispatcher.
     */
    @VisibleForTesting
    data class DispatcherConfig(
        val dispatcher: CoroutineDispatcher,
        val maxConcurrency: Int,
        val minConcurrency: Int,
        val pressureScaleDownThreshold: Float = PRESSURE_SCALE_DOWN_THRESHOLD,
        val pressureScaleUpThreshold: Float = PRESSURE_SCALE_UP_THRESHOLD,
    )

    /**
     * Permit that must be released when done.
     */
    class ConcurrencyPermit(
        private val semaphore: Semaphore,
        private val dispatcher: CoroutineDispatcher,
        private val controller: CoroutineConcurrencyController,
    ) : AutoCloseable {

        @Volatile
        private var released = false

        override fun close() {
            release()
        }

        fun release() {
            if (released) return
            released = true
            semaphore.release()
            controller.totalReleases.incrementAndGet()
        }

        val dispatcherName: String
            get() = dispatcher.name
    }

    /**
     * Exception thrown when permit acquisition times out.
     */
    class ConcurrencyTimeoutException(message: String) : Exception(message)

    /**
     * Statistics for concurrency controller.
     */
    data class ConcurrencyStats(
        val totalAcquires: Long,
        val totalReleases: Long,
        val totalTimeouts: Long,
        val currentWaiters: Int,
        val currentConcurrency: Map<CoroutineDispatcher, Int>,
        val maxConcurrency: Map<CoroutineDispatcher, Int>,
        val minConcurrency: Map<CoroutineDispatcher, Int>,
        val memoryPressure: Float,
        val isScalingDown: Boolean,
    ) {
        override fun toString(): String {
            return "ConcurrencyStats(acquires=$totalAcquires, releases=$totalReleases, timeouts=$totalTimeouts, " +
                    "waiters=$currentWaiters, pressure=${String.format("%.2f", memoryPressure)}, scalingDown=$isScalingDown, " +
                    "current=$currentConcurrency)"
        }
    }
}

/**
 * Extension functions for easier usage.
 */
suspend fun CoroutineScope.withConcurrency(
    controller: CoroutineConcurrencyController,
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
    block: suspend () -> Unit
) {
    controller.withPermit(dispatcher, block)
}

suspend fun <T> CoroutineScope.withConcurrency(
    controller: CoroutineConcurrencyController,
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
    block: suspend () -> T
): T {
    return controller.withPermit(dispatcher, block)
}