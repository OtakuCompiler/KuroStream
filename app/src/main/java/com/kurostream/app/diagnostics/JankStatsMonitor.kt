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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

class JankStatsMonitor(private val context: Context) {
    
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
        setupJankListener()
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
        
        jankStats.addJankListener(listener!!)
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
    
    fun getCurrentFps(): Float {
        val avgFrameTime = getAverageFrameTime()
        return if (avgFrameTime > 0) 1000f / avgFrameTime else 60f
    }
    
    fun resetStats() {
        _jankStats.value = JankStatsData()
        _frameTimeline.value = FrameTimelineData()
    }
    
    fun shutdown() {
        stopMonitoring()
        scope.cancel()
        jankStats.shutdown()
    }
    
    data class JankStatsData(
        val totalFrames: Long = 0,
        val jankFrames: Long = 0,
        val severeJankFrames: Long = 0,
        val totalFrameTimeMs: Float = 0f,
        val maxFrameTimeMs: Float = 0f,
        val lastJankTimeMs: Float = 0f,
        val lastFrameTimeMs: Float = 0f,
    ) {
        val jankPercentage: Float get() = if (totalFrames > 0) (jankFrames.toFloat() / totalFrames) * 100f else 0f
        val severeJankPercentage: Float get() = if (totalFrames > 0) (severeJankFrames.toFloat() / totalFrames) * 100f else 0f
        val averageFrameTimeMs: Float get() = if (totalFrames > 0) totalFrameTimeMs / totalFrames else 0f
        val currentFps: Float get() = if (averageFrameTimeMs > 0) 1000f / averageFrameTimeMs else 60f
    }
    
    data class FrameTimelineData(
        val frames: List<FrameData> = emptyList(),
    )
    
    data class FrameData(
        val timestamp: Long,
        val durationMs: Float,
        val isJank: Boolean,
        val isSevereJank: Boolean,
    )
}

class JankStatsOverlay @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val jankPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val severeJankPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val goodFramePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val fpsPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    
    private var jankStatsMonitor: JankStatsMonitor? = null
    private var isVisible = false
    
    private val padding = 16f
    private val graphHeight = 120f
    private val graphWidth = 300f
    private val barWidth = 2f
    private val barSpacing = 1f
    
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
            typeface = android.graphics.Typeface.MONOSPACE
        }
        
        fpsPaint.apply {
            color = Color.CYAN
            textSize = 16f
            typeface = android.graphics.Typeface.MONOSPACE
            isFakeBoldText = true
        }
        
        goodFramePaint.apply {
            color = Color.GREEN
            style = Paint.Style.FILL
        }
        
        jankPaint.apply {
            color = Color.YELLOW
            style = Paint.Style.FILL
        }
        
        severeJankPaint.apply {
            color = Color.RED
            style = Paint.Style.FILL
        }
    }
    
    fun attachMonitor(monitor: JankStatsMonitor) {
        jankStatsMonitor = monitor
    }
    
    fun setVisible(visible: Boolean) {
        isVisible = visible
        visibility = if (visible) VISIBLE else GONE
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        if (!isVisible || jankStatsMonitor == null) return
        
        val stats = jankStatsMonitor!!.getCurrentJankStats()
        val timeline = jankStatsMonitor!!.getFrameTimeline()
        
        val w = width.toFloat()
        val h = height.toFloat()
        
        // Draw background
        canvas.drawRect(0f, 0f, w, h, backgroundPaint)
        
        var y = padding + 24f
        
        // Draw FPS
        textPaint.color = Color.CYAN
        textPaint.textSize = 18f
        canvas.drawText("JANK STATS", padding, y, textPaint)
        y += 28f
        
        textPaint.textSize = 14f
        textPaint.color = Color.WHITE
        
        val fps = jankStatsMonitor!!.getCurrentFps()
        val fpsColor = when {
            fps >= 55f -> Color.GREEN
            fps >= 45f -> Color.YELLOW
            else -> Color.RED
        }
        fpsPaint.color = fpsColor
        canvas.drawText("FPS: ${fps.roundToInt()}", padding, y, fpsPaint)
        y += 24f
        
        canvas.drawText("Avg Frame: ${jankStatsMonitor!!.getAverageFrameTime().roundToInt()}ms", padding, y, textPaint)
        y += 20f
        
        canvas.drawText("Jank: ${jankStatsMonitor!!.getJankPercentage().roundToInt()}% (${stats.jankFrames}/${stats.totalFrames})", padding, y, textPaint)
        y += 20f
        
        canvas.drawText("Severe Jank: ${jankStatsMonitor!!.getSevereJankPercentage().roundToInt()}% (${stats.severeJankFrames})", padding, y, textPaint)
        y += 20f
        
        canvas.drawText("Max Frame: ${stats.maxFrameTimeMs.roundToInt()}ms", padding, y, textPaint)
        y += 30f
        
        // Draw frame timeline graph
        drawFrameGraph(canvas, timeline, w - graphWidth - padding, padding + 24f)
    }
    
    private fun drawFrameGraph(
        canvas: Canvas,
        timeline: JankStatsMonitor.FrameTimelineData,
        startX: Float,
        startY: Float
    ) {
        val frames = timeline.frames
        if (frames.isEmpty()) return
        
        val maxBars = ((graphWidth) / (barWidth + barSpacing)).toInt()
        val framesToDraw = if (frames.size > maxBars) {
            frames.takeLast(maxBars)
        } else {
            frames
        }
        
        canvas.drawRect(
            startX, startY,
            startX + graphWidth, startY + graphHeight,
            backgroundPaint
        )
        
        var x = startX
        framesToDraw.forEach { frame ->
            val barHeight = (frame.durationMs / 50f * graphHeight).coerceAtMost(graphHeight).coerceAtLeast(2f)
            val barY = startY + graphHeight - barHeight
            
            val barPaint = when {
                frame.isSevereJank -> severeJankPaint
                frame.isJank -> jankPaint
                else -> goodFramePaint
            }
            
            canvas.drawRect(x, barY, x + barWidth, startY + graphHeight, barPaint)
            x += barWidth + barSpacing
        }
        
        // Draw threshold lines
        val jankThresholdY = startY + graphHeight - (jankThresholdMs / 50f * graphHeight)
        val severeJankThresholdY = startY + graphHeight - (severeJankThresholdMs / 50f * graphHeight)
        
        paint.color = Color.parseColor("#FFFFFFFF")
        paint.strokeWidth = 1f
        paint.style = Paint.Style.STROKE
        paint.pathEffect = android.graphics.DashPathEffect(floatArrayOf(5f, 5f), 0f)
        
        canvas.drawLine(startX, jankThresholdY, startX + graphWidth, jankThresholdY, paint)
        canvas.drawLine(startX, severeJankThresholdY, startX + graphWidth, severeJankThresholdY, paint)
        
        paint.pathEffect = null
        
        // Labels
        textPaint.textSize = 10f
        textPaint.color = Color.YELLOW
        canvas.drawText("Jank (16.67ms)", startX, jankThresholdY - 4, textPaint)
        textPaint.color = Color.RED
        canvas.drawText("Severe (33.33ms)", startX, severeJankThresholdY - 4, textPaint)
    }
    
    companion object {
        private const val jankThresholdMs = 16.67f
        private const val severeJankThresholdMs = 33.33f
    }
}

@Composable
fun JankStatsOverlayCompose(
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier,
    jankStatsMonitor: JankStatsMonitor,
    isVisible: Boolean = true,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    
    androidx.compose.ui.viewinterop.AndroidView(
        modifier = modifier,
        factory = { ctx ->
            JankStatsOverlay(ctx).apply {
                attachMonitor(jankStatsMonitor)
                setVisible(isVisible)
            }
        },
        update = { view ->
            view.setVisible(isVisible)
        }
    )
}

class FrameTimeInterceptor : androidx.jankstats.JankStatsOnFrameListener {
    override fun onFrame(frameData: androidx.jankstats.FrameData) {
        // This is called by JankStats for each frame
    }
}

class JankStatsReporter(
    private val monitor: JankStatsMonitor,
    private val intervalMs: Long = 5000,
) {
    private var reportJob: kotlinx.coroutines.Job? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    fun startReporting() {
        reportJob?.cancel()
        reportJob = scope.launch {
            while (true) {
                kotlinx.coroutines.delay(intervalMs)
                report()
            }
        }
    }
    
    fun stopReporting() {
        reportJob?.cancel()
        reportJob = null
    }
    
    private fun report() {
        val stats = monitor.getCurrentJankStats()
        val timeline = monitor.getFrameTimeline()
        
        val recentFrames = timeline.frames.takeLast(60)
        val recentJank = recentFrames.count { it.isJank }
        val recentSevereJank = recentFrames.count { it.isSevereJank }
        
        Timber.i("""
            JankStats Report:
            - FPS: ${monitor.getCurrentFps().roundToInt()}
            - Avg Frame: ${monitor.getAverageFrameTime().roundToInt()}ms
            - Total Frames: ${stats.totalFrames}
            - Total Jank: ${stats.jankFrames} (${monitor.getJankPercentage().roundToInt()}%)
            - Severe Jank: ${stats.severeJankFrames} (${monitor.getSevereJankPercentage().roundToInt()}%)
            - Max Frame: ${stats.maxFrameTimeMs.roundToInt()}ms
            - Recent (60 frames): Jank=$recentJank, Severe=$recentSevereJank
        """.trimIndent())
    }
    
    fun shutdown() {
        stopReporting()
        scope.cancel()
    }
}