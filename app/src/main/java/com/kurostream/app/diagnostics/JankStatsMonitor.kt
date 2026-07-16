package com.kurostream.app.diagnostics

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.view.ViewCompat
import androidx.jankstats.JankStats
import androidx.jankstats.JankStatsListener
import androidx.jankstats.JankStatsOnFrameListener
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

class JankStatsMonitor(
    private val context: Context,
    private val lifecycle: Lifecycle
) : DefaultLifecycleObserver {

    private val jankStats = JankStats.createAndTrack(context)

    private val _jankStats = MutableStateFlow(JankStatsData())
    val jankStats: StateFlow<JankStatsData> = _jankStats.asStateFlow()

    private val _frameTimeline = MutableStateFlow(FrameTimelineData())
    val frameTimeline: StateFlow<FrameTimelineData> = _frameTimeline.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var listener: JankStatsListener? = null
    private var isMonitoring = false

    private val jankThresholdMs = 16.67f // 60fps = 16.67ms per frame
    private val severeJankThresholdMs = 33.33f // 30fps = 33.33ms per frame

    init {
        lifecycle.addObserver(this)
        setupJankListener()
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

    private fun setupJankListener() {
        listener = JankStatsListener { jankType, frameData ->
            val isJank = frameData.durationMs > jankThresholdMs
            val isSevereJank = frameData.durationMs > severeJankThresholdMs

            scope.launch {
                _jankStats.update { current ->
                    current.copy(
                        totalFrames = current.totalFrames + 1,
                        jankFrames = current.jankFrames + (if (isJank) 1 else 0),
                        severeJankFrames = current.severeJankFrames + (if (isSevereJank) 1 else 0),
                        totalFrameTimeMs = current.totalFrameTimeMs + frameData.durationMs,
                        maxFrameTimeMs = maxOf(current.maxFrameTimeMs, frameData.durationMs),
                        lastJankTimeMs = if (isJank) frameData.durationMs else current.lastJankTimeMs,
                        lastFrameTimeMs = frameData.durationMs,
                    )
                }

                _frameTimeline.update { current ->
                    val newFrames = current.frames + FrameData(
                        timestamp = frameData.frameStartTimeMs,
                        durationMs = frameData.durationMs,
                        isJank = isJank,
                        isSevereJank = isSevereJank,
                    )

                    // Keep last 300 frames (5 seconds at 60fps)
                    val trimmedFrames = if (newFrames.size > 300) {
                        newFrames.drop(newFrames.size - 300)
                    } else {
                        newFrames
                    }

                    current.copy(frames = trimmedFrames)
                }
            }

            if (isSevereJank) {
                Timber.w("Severe jank detected: ${frameData.durationMs}ms (threshold: ${severeJankThresholdMs}ms)")
            } else if (isJank) {
                Timber.d("Jank detected: ${frameData.durationMs}ms")
            }
        }

        jankStats.addJankListener(listener)
        isMonitoring = true
    }

    fun startMonitoring() {
        if (!isMonitoring) {
            listener?.let { jankStats.addJankListener(it) }
            isMonitoring = true
        }
    }

    fun stopMonitoring() {
        listener?.let { jankStats.removeJankListener(it) }
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
        modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier,
        jankStatsMonitor: JankStatsMonitor
    ) {
        val jankStatsData by jankStatsMonitor.jankStats.collectAsStateWithLifecycle()
        val frameTimelineData by jankStatsMonitor.frameTimeline.collectAsStateWithLifecycle()

        androidx.compose.material3.Surface(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp),
            color = androidx.compose.ui.graphics.Color(0x80000000),
            shape = androidx.compose.ui.graphics.shape.RoundedCornerShape(12.dp)
        ) {
            androidx.compose.foundation.layout.Column(
                modifier = androidx.compose.ui.Modifier.padding(16.dp),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
            ) {
                androidx.compose.material3.Text(
                    text = "Jank Stats",
                    style = androidx.compose.material3.Typography().titleMedium,
                    color = androidx.compose.ui.graphics.Color.White
                )
                androidx.compose.material3.Text(
                    text = "Jank: ${jankStatsMonitor.getJankPercentage().toStringAsFixed(1)}%",
                    style = androidx.compose.material3.Typography().bodyLarge,
                    color = androidx.compose.ui.graphics.Color.White
                )
                androidx.compose.material3.Text(
                    text = "Severe: ${jankStatsMonitor.getSevereJankPercentage().toStringAsFixed(1)}%",
                    style = androidx.compose.material3.Typography().bodyLarge,
                    color = androidx.compose.ui.graphics.Color.Red
                )
                androidx.compose.material3.Text(
                    text = "Avg Frame: ${jankStatsMonitor.getAverageFrameTime().toStringAsFixed(1)}ms",
                    style = androidx.compose.material3.Typography().bodyLarge,
                    color = androidx.compose.ui.graphics.Color.White
                )
                androidx.compose.material3.Text(
                    text = "Frames: ${jankStatsData.totalFrames}",
                    style = androidx.compose.material3.Typography().bodyMedium,
                    color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f)
                )
            }
        }
    }
}