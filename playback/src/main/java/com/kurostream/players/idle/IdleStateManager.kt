package com.kurostream.players.idle

import android.content.Context
import android.view.Choreographer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IdleStateManager @Inject constructor(
    private val context: Context
) : Choreographer.FrameCallback {

    private val _idleState = MutableStateFlow(IdleState())
    val idleState: StateFlow<IdleState> = _idleState.asStateFlow()

    private val choreographer = Choreographer.getInstance()
    private var lastFrameTime = 0L
    private var frameCount = 0
    private var fps = 60f

    @Volatile private var isForeground = true
    @Volatile private var isScrolling = false
    @Volatile private var lastInteractionTime = System.currentTimeMillis()

    private val memoryCallbacks = mutableListOf<() -> Unit>()

    fun start() {
        choreographer.postFrameCallback(this)
        Timber.d("IdleStateManager started")
    }

    fun stop() {
        choreographer.removeFrameCallback(this)
        Timber.d("IdleStateManager stopped")
    }

    override fun doFrame(frameTimeNanos: Long) {
        choreographer.postFrameCallback(this)

        val frameTimeMs = frameTimeNanos / 1_000_000
        if (lastFrameTime > 0) {
            val frameDelta = frameTimeMs - lastFrameTime
            if (frameDelta > 0 && frameDelta < 1000) {
                frameCount++
                if (frameCount % 10 == 0) {
                    fps = 1000f / frameDelta
                    detectScrollState(frameDelta)
                }
            }
        }
        lastFrameTime = frameTimeMs

        checkIdleState()
    }

    private fun detectScrollState(frameDelta: Float) {
        val wasScrolling = isScrolling
        isScrolling = frameDelta > 20f

        if (isScrolling != wasScrolling) {
            Timber.d("Scroll state: $isScrolling (frame: ${frameDelta.toInt()}ms)")
            if (isScrolling) {
                onScrollStart()
            } else {
                onScrollEnd()
            }
        }
    }

    private fun onScrollStart() {
        _idleState.update { it.copy(isScrolling = true, scrollStartTime = System.currentTimeMillis()) }
        Timber.d("Scroll started (RAM budget: 150MB)")
    }

    private fun onScrollEnd() {
        val scrollDuration = System.currentTimeMillis() - _idleState.value.scrollStartTime
        _idleState.update { it.copy(isScrolling = false, lastScrollDuration = scrollDuration) }
        Timber.d("Scroll ended (duration: ${scrollDuration}ms, triggering GC in 2s)")

        scheduleIdleGC(2000)
    }

    private fun checkIdleState() {
        val timeSinceInteraction = System.currentTimeMillis() - lastInteractionTime
        val wasIdle = _idleState.value.isIdle
        val isNowIdle = timeSinceInteraction > 5000 && !isScrolling && isForeground

        if (isNowIdle != wasIdle) {
            _idleState.update { it.copy(
                isIdle = isNowIdle,
                idleStartTime = if (isNowIdle) System.currentTimeMillis() else 0,
                idleDuration = if (wasIdle) System.currentTimeMillis() - it.idleStartTime else 0,
            )}

            if (isNowIdle) {
                onIdleStart()
            } else {
                onIdleEnd()
            }
        }
    }

    private fun onIdleStart() {
        Timber.d("Idle state started (RAM budget: 50MB)")
        memoryCallbacks.forEach { it() }
    }

    private fun onIdleEnd() {
        Timber.d("Idle state ended")
    }

    fun onUserInteraction() {
        lastInteractionTime = System.currentTimeMillis()
        if (_idleState.value.isIdle) {
            _idleState.update { it.copy(isIdle = false, idleStartTime = 0) }
            onIdleEnd()
        }
    }

    private fun scheduleIdleGC(delayMs: Long) {
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            if (!isScrolling && _idleState.value.isIdle) {
                // Let system handle GC naturally
                Timber.d("Idle GC opportunity")
            }
        }, delayMs)
    }

    fun registerMemoryCallback(callback: () -> Unit) {
        memoryCallbacks.add(callback)
    }

    fun unregisterMemoryCallback(callback: () -> Unit) {
        memoryCallbacks.remove(callback)
    }

    fun setForeground(isForeground: Boolean) {
        this.isForeground = isForeground
        if (!isForeground) {
            onIdleStart()
        }
    }

    fun getCurrentFps(): Float = fps

    fun getRamBudgetMb(): Int = when {
        isScrolling -> 150
        _idleState.value.isIdle -> 50
        isForeground -> 100
        else -> 75
    }
}

data class IdleState(
    val isIdle: Boolean = false,
    val isScrolling: Boolean = false,
    val idleStartTime: Long = 0,
    val idleDuration: Long = 0,
    val scrollStartTime: Long = 0,
    val lastScrollDuration: Long = 0,
) {
    val currentIdleDurationMs: Long
        get() = if (isIdle) System.currentTimeMillis() - idleStartTime else 0
}
