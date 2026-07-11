// This file is part of KuroStream.
//
// KuroStream is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// KuroStream is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with KuroStream.  If not, see <https://www.gnu.org/licenses/>.

package com.kurostream.players.diagnostics

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.lifecycle.LifecycleCoroutineScope
import com.kurostream.players.core.PlayerInterface
import com.kurostream.players.core.PlaybackDiagnostics
import com.kurostream.players.core.PlaybackState
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.math.roundToInt

class DiagnosticsOverlay @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val warningPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var diagnostics = PlaybackDiagnostics()
    private var playbackState: PlaybackState = PlaybackState.Idle
    private var positionMs = 0L
    private var durationMs = 0L

    private val fpsHistory = ArrayDeque<Float>(60)
    private val bufferHistory = ArrayDeque<Long>(60)
    private val bitrateHistory = ArrayDeque<Long>(60)

    private var updateJob: Job? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val padding = 16f
    private val lineHeight = 28f
    private val graphHeight = 80f
    private val graphWidth = 200f

    init {
        setupPaints()
        setLayerType(LAYER_TYPE_HARDWARE, null)
    }

    private fun setupPaints() {
        backgroundPaint.apply {
            color = Color.parseColor("#CC000000")
            style = Paint.Style.FILL
        }
        textPaint.apply {
            color = Color.WHITE
            textSize = 14f
            typeface = Typeface.MONOSPACE
        }
        paint.apply {
            color = Color.GREEN
            strokeWidth = 2f
            style = Paint.Style.STROKE
        }
        warningPaint.apply {
            color = Color.YELLOW
            textSize = 14f
            typeface = Typeface.MONOSPACE
        }
        gridPaint.apply {
            color = Color.parseColor("#33FFFFFF")
            strokeWidth = 1f
            style = Paint.Style.STROKE
        }
    }

    fun attachPlayer(player: PlayerInterface, lifecycleScope: LifecycleCoroutineScope) {
        updateJob?.cancel()
        updateJob = lifecycleScope.launch {
            launch {
                player.diagnostics.collect { diag ->
                    diagnostics = diag
                    updateHistory(diag)
                    invalidate()
                }
            }
            launch {
                player.playbackState.collect { state ->
                    playbackState = state
                    invalidate()
                }
            }
            launch {
                player.positionMs.collect { pos ->
                    positionMs = pos
                    invalidate()
                }
            }
            launch {
                player.durationMs.collect { dur ->
                    durationMs = dur
                    invalidate()
                }
            }
        }
    }

    fun detachPlayer() {
        updateJob?.cancel()
        updateJob = null
    }

    private fun updateHistory(diag: PlaybackDiagnostics) {
        fpsHistory.addLast(diag.currentFps)
        if (fpsHistory.size > 60) fpsHistory.removeFirst()
        bufferHistory.addLast(diag.bufferDurationMs)
        if (bufferHistory.size > 60) bufferHistory.removeFirst()
        bitrateHistory.addLast(diag.currentBitrate / 1000)
        if (bitrateHistory.size > 60) bitrateHistory.removeFirst()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        canvas.drawRect(0f, 0f, w, h, backgroundPaint)

        var y = padding + lineHeight
        val x = padding

        textPaint.textSize = 16f
        textPaint.color = Color.CYAN
        canvas.drawText("PLAYBACK DIAGNOSTICS", x, y, textPaint)
        y += lineHeight * 1.5f

        textPaint.textSize = 14f
        textPaint.color = Color.WHITE

        canvas.drawText("=== STATE ===", x, y, textPaint)
        y += lineHeight
        val stateName = playbackState.javaClass.simpleName?.replace("$", "") ?: "Unknown"
        canvas.drawText("State: $stateName", x, y, textPaint)
        y += lineHeight
        canvas.drawText("Position: ${formatTime(positionMs)} / ${formatTime(durationMs)}", x, y, textPaint)
        y += lineHeight
        val progress = if (durationMs > 0) (positionMs * 100 / durationMs) else 0
        canvas.drawText("Progress: ${progress}%", x, y, textPaint)
        y += lineHeight * 1.5f

        canvas.drawText("=== VIDEO ===", x, y, textPaint)
        y += lineHeight
        canvas.drawText("Resolution: ${diagnostics.videoResolution}", x, y, textPaint)
        y += lineHeight
        canvas.drawText("Codec: ${diagnostics.videoCodec}", x, y, textPaint)
        y += lineHeight
        canvas.drawText("Decoder: ${diagnostics.decoderName}", x, y, textPaint)
        y += lineHeight
        canvas.drawText("HW Decode: ${if (diagnostics.isHardwareDecoding) "YES" else "NO"}", x, y, textPaint)
        y += lineHeight
        canvas.drawText("FPS: ${diagnostics.currentFps.roundToInt()} (content: ${diagnostics.contentFrameRate.roundToInt()})", x, y, textPaint)
        y += lineHeight * 1.5f

        canvas.drawText("=== BUFFER ===", x, y, textPaint)
        y += lineHeight
        canvas.drawText("Buffer: ${diagnostics.bufferDurationMs}ms (${diagnostics.bufferedPercentage}%)", x, y, textPaint)
        y += lineHeight
        canvas.drawText("Dropped: ${diagnostics.droppedFrames} | Rendered: ${diagnostics.renderedFrames}", x, y, textPaint)
        y += lineHeight
        canvas.drawText("Bitrate: ${formatBitrate(diagnostics.currentBitrate)}", x, y, textPaint)
        y += lineHeight
        canvas.drawText("Network: ${formatBitrate(diagnostics.networkSpeedBps)}", x, y, textPaint)
        y += lineHeight * 1.5f

        canvas.drawText("=== DISPLAY ===", x, y, textPaint)
        y += lineHeight
        canvas.drawText("Refresh: ${diagnostics.displayRefreshRate.roundToInt()}Hz", x, y, textPaint)
        y += lineHeight
        canvas.drawText("Audio: ${diagnostics.audioCodec}", x, y, textPaint)
        y += lineHeight

        if (diagnostics.droppedFrames > 10) {
            warningPaint.color = Color.RED
            canvas.drawText("FRAME DROPS DETECTED", x, y, warningPaint)
            y += lineHeight
        }
        if (diagnostics.bufferDurationMs < 3000) {
            warningPaint.color = Color.YELLOW
            canvas.drawText("LOW BUFFER", x, y, warningPaint)
            y += lineHeight
        }
        if (!diagnostics.isHardwareDecoding && diagnostics.videoResolution.contains("1080")) {
            warningPaint.color = Color.YELLOW
            canvas.drawText("SW DECODE (1080p+)", x, y, warningPaint)
            y += lineHeight
        }

        drawGraphs(canvas, w - graphWidth - padding, padding + lineHeight)
    }

    private fun drawGraphs(canvas: Canvas, startX: Float, startY: Float) {
        drawGraph(canvas, "FPS", fpsHistory, startX, startY, graphWidth, graphHeight, 0f, 120f, Color.GREEN)
        drawGraph(canvas, "Buffer (ms)", bufferHistory, startX, startY + graphHeight + 20, graphWidth, graphHeight, 0f, 30000f, Color.CYAN)
        drawGraph(canvas, "Bitrate (kbps)", bitrateHistory, startX, startY + (graphHeight + 20) * 2, graphWidth, graphHeight, 0f, 50000f, Color.MAGENTA)
    }

    private fun <T : Number> drawGraph(
        canvas: Canvas, label: String, data: ArrayDeque<T>,
        x: Float, y: Float, w: Float, h: Float,
        minVal: Float, maxVal: Float, color: Int
    ) {
        canvas.drawRect(x, y, x + w, y + h, backgroundPaint)
        gridPaint.color = Color.parseColor("#22FFFFFF")
        for (i in 0..4) {
            val gy = y + h * i / 4
            canvas.drawLine(x, gy, x + w, gy, gridPaint)
        }
        textPaint.textSize = 12f
        textPaint.color = color
        canvas.drawText(label, x, y - 4, textPaint)

        if (data.size >= 2) {
            paint.color = color
            paint.strokeWidth = 2f
            val stepX = w / (data.size - 1).coerceAtLeast(1)
            val range = maxVal - minVal
            val path = android.graphics.Path()
            data.forEachIndexed { index, value ->
                val vx = x + index * stepX
                val normalized = ((value.toFloat() - minVal) / range).coerceIn(0f, 1f)
                val vy = y + h - (normalized * h)
                if (index == 0) path.moveTo(vx, vy) else path.lineTo(vx, vy)
            }
            canvas.drawPath(path, paint)
            data.lastOrNull()?.let { last ->
                textPaint.textSize = 12f
                textPaint.color = color
                canvas.drawText("${last.toInt()}", x + w + 4, y + h / 2, textPaint)
            }
        }
    }

    private fun formatTime(ms: Long): String {
        if (ms <= 0) return "00:00:00"
        val seconds = ms / 1000
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        return String.format("%02d:%02d:%02d", hours, minutes, secs)
    }

    private fun formatBitrate(bps: Long): String {
        return when {
            bps >= 1_000_000_000 -> "${(bps / 1_000_000_000.0).roundToInt()} Gbps"
            bps >= 1_000_000 -> "${(bps / 1_000_000.0).roundToInt()} Mbps"
            bps >= 1_000 -> "${(bps / 1_000.0).roundToInt()} kbps"
            else -> "$bps bps"
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        detachPlayer()
    }
}

class DiagnosticsManager(
    private val context: Context,
    private val player: PlayerInterface
) {
    private val _isOverlayVisible = MutableStateFlow(false)
    val isOverlayVisible: StateFlow<Boolean> = _isOverlayVisible.asStateFlow()

    private val _isDeveloperMode = MutableStateFlow(false)
    val isDeveloperMode: StateFlow<Boolean> = _isDeveloperMode.asStateFlow()

    fun enableDeveloperMode() {
        _isDeveloperMode.value = true
        player.enableDiagnosticsOverlay(true)
    }

    fun disableDeveloperMode() {
        _isDeveloperMode.value = false
        _isOverlayVisible.value = false
        player.enableDiagnosticsOverlay(false)
    }

    fun toggleOverlay() {
        if (!_isDeveloperMode.value) return
        _isOverlayVisible.value = !_isOverlayVisible.value
        player.enableDiagnosticsOverlay(_isOverlayVisible.value)
    }

    fun showOverlay() {
        if (_isDeveloperMode.value) {
            _isOverlayVisible.value = true
            player.enableDiagnosticsOverlay(true)
        }
    }

    fun hideOverlay() {
        _isOverlayVisible.value = false
        player.enableDiagnosticsOverlay(false)
    }

    fun exportDiagnostics(): String {
        val diag = player.diagnostics.value
        return buildString {
            appendLine("{")
            appendLine("  \"timestamp\": ${diag.timestamp},")
            appendLine("  \"buffer_duration_ms\": ${diag.bufferDurationMs},")
            appendLine("  \"buffered_percentage\": ${diag.bufferedPercentage},")
            appendLine("  \"current_bitrate\": ${diag.currentBitrate},")
            appendLine("  \"dropped_frames\": ${diag.droppedFrames},")
            appendLine("  \"rendered_frames\": ${diag.renderedFrames},")
            appendLine("  \"current_fps\": ${diag.currentFps},")
            appendLine("  \"display_refresh_rate\": ${diag.displayRefreshRate},")
            appendLine("  \"content_frame_rate\": ${diag.contentFrameRate},")
            appendLine("  \"network_speed_bps\": ${diag.networkSpeedBps},")
            appendLine("  \"decoder_name\": \"${diag.decoderName}\",")
            appendLine("  \"video_codec\": \"${diag.videoCodec}\",")
            appendLine("  \"audio_codec\": \"${diag.audioCodec}\",")
            appendLine("  \"video_resolution\": \"${diag.videoResolution}\",")
            appendLine("  \"hardware_decoding\": ${diag.isHardwareDecoding}")
            appendLine("}")
        }
    }
}
