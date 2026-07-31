package com.kurostream.app.diagnostics

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

data class JankStatsData(
    val totalFrames: Int = 0,
    val jankFrames: Int = 0,
    val severeJankFrames: Int = 0,
    val totalFrameTimeMs: Float = 0f,
    val maxFrameTimeMs: Float = 0f,
    val lastJankTimeMs: Float = 0f,
    val lastFrameTimeMs: Float = 0f,
)

data class FrameData(
    val timestamp: Long,
    val durationMs: Float,
    val isJank: Boolean,
    val isSevereJank: Boolean,
)

data class FrameTimelineData(
    val frames: List<FrameData> = emptyList(),
)

class JankStatsMonitor(
    private val context: Context,
    private val lifecycle: Lifecycle
) : DefaultLifecycleObserver {

    private val _jankStats = MutableStateFlow(JankStatsData())
    val jankStats: StateFlow<JankStatsData> = _jankStats.asStateFlow()

    private val _frameTimeline = MutableStateFlow(FrameTimelineData())
    val frameTimeline: StateFlow<FrameTimelineData> = _frameTimeline.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var isMonitoring = false

    private val jankThresholdMs = 16.67f // 60fps = 16.67ms per frame
    private val severeJankThresholdMs = 33.33f // 30fps = 33.33ms per frame

    init {
        lifecycle.addObserver(this)
    }

    override fun onStart(owner: LifecycleOwner) {
        startMonitoring()
    }

    override fun onStop(owner: LifecycleOwner) {
        stopMonitoring()
    }

    override fun onDestroy(owner: LifecycleOwner) {
        lifecycle.removeObserver(this)
        scope.cancel()
        stopMonitoring()
    }

    fun recordFrame(frameTimeMs: Float) {
        val isJank = frameTimeMs > jankThresholdMs
        val isSevereJank = frameTimeMs > severeJankThresholdMs

        scope.launch {
            _jankStats.update { current ->
                current.copy(
                    totalFrames = current.totalFrames + 1,
                    jankFrames = current.jankFrames + (if (isJank) 1 else 0),
                    severeJankFrames = current.severeJankFrames + (if (isSevereJank) 1 else 0),
                    totalFrameTimeMs = current.totalFrameTimeMs + frameTimeMs,
                    maxFrameTimeMs = maxOf(current.maxFrameTimeMs, frameTimeMs),
                    lastJankTimeMs = if (isJank) frameTimeMs else current.lastJankTimeMs,
                    lastFrameTimeMs = frameTimeMs,
                )
            }

            _frameTimeline.update { current ->
                val newFrames = current.frames + FrameData(
                    timestamp = System.currentTimeMillis(),
                    durationMs = frameTimeMs,
                    isJank = isJank,
                    isSevereJank = isSevereJank,
                )

                // Keep last 60 frames (1 second at 60fps)
                // NOTE: JankStats API should be wired in onResume/onPause instead of lifecycle observer
                val trimmedFrames = if (newFrames.size > 60) {
                    newFrames.drop(newFrames.size - 60)
                } else {
                    newFrames
                }

                current.copy(frames = trimmedFrames)
            }
        }

        if (isSevereJank) {
            Timber.w("Severe jank detected: ${formatFloat(frameTimeMs)}ms (threshold: ${formatFloat(severeJankThresholdMs)}ms)")
        } else if (isJank) {
            Timber.d("Jank detected: ${formatFloat(frameTimeMs)}ms")
        }
    }

    fun startMonitoring() {
        isMonitoring = true
    }

    fun stopMonitoring() {
        isMonitoring = false
    }

    fun getCurrentJankStats(): JankStatsData = _jankStats.value

    fun getFrameTimeline(): FrameTimelineData = _frameTimeline.value

    fun getJankPercentage(): Float {
        val data = _jankStats.value
        return if (data.totalFrames > 0) {
            (data.jankFrames.toFloat() / data.totalFrames) * 100f
        } else 0f
    }

    fun getSevereJankPercentage(): Float {
        val data = _jankStats.value
        return if (data.totalFrames > 0) {
            (data.severeJankFrames.toFloat() / data.totalFrames) * 100f
        } else 0f
    }

    fun getAverageFrameTime(): Float {
        val data = _jankStats.value
        return if (data.totalFrames > 0) {
            data.totalFrameTimeMs / data.totalFrames
        } else 0f
    }

    @Composable
    fun JankStatsOverlay(
        modifier: Modifier = Modifier,
    ) {
        val jankStatsData by jankStats.collectAsState()
        val frameTimelineData by frameTimeline.collectAsState()

        Surface(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp),
            color = Color(0x80000000),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Jank Stats",
                    style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
                Text(
                    text = "Jank: ${formatFloat(getJankPercentage())}%",
                    style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
                    color = Color.White
                )
                Text(
                    text = "Severe: ${formatFloat(getSevereJankPercentage())}%",
                    style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
                    color = Color.Red
                )
                Text(
                    text = "Avg Frame: ${formatFloat(getAverageFrameTime())}ms",
                    style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
                    color = Color.White
                )
                Text(
                    text = "Frames: ${jankStatsData.totalFrames}",
                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }
    }

    private fun formatFloat(value: Float): String {
        return String.format("%.1f", value)
    }
}
