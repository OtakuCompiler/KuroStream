package com.kurostream.tv.core.player

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import com.kurostream.tv.data.remote.skip.SkipTimestamp
import com.kurostream.tv.data.remote.skip.SkipTimestampService
import com.kurostream.tv.data.remote.skip.SkipType
import timber.log.Timber
import javax.inject.Inject

/**
 * Controller for managing skip overlay display and interactions
 * Handles auto-skip functionality and skip button visibility
 */
class SkipOverlayController @Inject constructor(
    private val skipTimestampService: SkipTimestampService
) {
    companion object {
        private const val TAG = "SkipOverlayController"
        private const val POSITION_UPDATE_INTERVAL_MS = 250L
        private const val SKIP_BUTTON_LEAD_TIME = 2.0 // Show button 2 seconds before segment
        private const val AUTO_SKIP_DELAY_MS = 3000L // Auto-skip after 3 seconds if enabled
    }
    
    private val _skipState = MutableStateFlow(SkipOverlayState())
    val skipState: StateFlow<SkipOverlayState> = _skipState.asStateFlow()
    
    private var currentTimestamps: List<SkipTimestamp> = emptyList()
    private var positionMonitorJob: Job? = null
    private var autoSkipJob: Job? = null
    
    private var autoSkipEnabled: Boolean = false
    private var autoSkipOpenings: Boolean = true
    private var autoSkipEndings: Boolean = true
    private var autoSkipRecaps: Boolean = true
    
    /**
     * Load skip timestamps for an episode
     */
    suspend fun loadSkipTimestamps(
        malId: Long,
        episodeNumber: Int,
        episodeLength: Long? = null
    ) {
        _skipState.value = _skipState.value.copy(isLoading = true)
        
        skipTimestampService.getSkipTimestamps(malId, episodeNumber, episodeLength)
            .onSuccess { timestamps ->
                currentTimestamps = timestamps
                _skipState.value = _skipState.value.copy(
                    isLoading = false,
                    hasTimestamps = timestamps.isNotEmpty(),
                    availableSkips = timestamps.map { it.type }.distinct()
                )
                Timber.tag(TAG).d("Loaded ${timestamps.size} skip timestamps")
            }
            .onFailure { error ->
                Timber.tag(TAG).e(error, "Failed to load skip timestamps")
                _skipState.value = _skipState.value.copy(
                    isLoading = false,
                    hasTimestamps = false,
                    error = error.message
                )
            }
    }
    
    /**
     * Start monitoring playback position for skip segments
     */
    fun startMonitoring(
        scope: CoroutineScope,
        getPosition: () -> Long
    ) {
        stopMonitoring()
        
        positionMonitorJob = scope.launch {
            while (isActive) {
                val positionMs = getPosition()
                val positionSeconds = positionMs / 1000.0
                
                updateSkipState(positionSeconds)
                delay(POSITION_UPDATE_INTERVAL_MS)
            }
        }
    }
    
    /**
     * Stop monitoring playback position
     */
    fun stopMonitoring() {
        positionMonitorJob?.cancel()
        positionMonitorJob = null
        autoSkipJob?.cancel()
        autoSkipJob = null
    }
    
    /**
     * Configure auto-skip settings
     */
    fun configureAutoSkip(
        enabled: Boolean,
        skipOpenings: Boolean = true,
        skipEndings: Boolean = true,
        skipRecaps: Boolean = true
    ) {
        autoSkipEnabled = enabled
        autoSkipOpenings = skipOpenings
        autoSkipEndings = skipEndings
        autoSkipRecaps = skipRecaps
    }
    
    /**
     * Manually skip current segment
     * @return The end position in milliseconds to seek to, or null if no active segment
     */
    fun skipCurrentSegment(): Long? {
        val activeSegment = _skipState.value.activeSegment ?: return null
        
        // Mark as manually skipped
        _skipState.value = _skipState.value.copy(
            activeSegment = null,
            showSkipButton = false,
            lastSkippedType = activeSegment.type
        )
        
        Timber.tag(TAG).d("Manually skipped ${activeSegment.type.displayName}")
        return (activeSegment.endTime * 1000).toLong()
    }
    
    /**
     * Dismiss the skip button without skipping
     */
    fun dismissSkipButton() {
        _skipState.value = _skipState.value.copy(
            showSkipButton = false,
            dismissed = true
        )
        autoSkipJob?.cancel()
    }
    
    /**
     * Get the skip position for auto-skip
     * @return The end position in milliseconds, or null if auto-skip is not applicable
     */
    fun getAutoSkipPosition(): Long? {
        if (!autoSkipEnabled) return null
        
        val activeSegment = _skipState.value.activeSegment ?: return null
        
        val shouldAutoSkip = when (activeSegment.type) {
            SkipType.OPENING -> autoSkipOpenings
            SkipType.ENDING -> autoSkipEndings
            SkipType.RECAP -> autoSkipRecaps
            SkipType.UNKNOWN -> false
        }
        
        return if (shouldAutoSkip) {
            (activeSegment.endTime * 1000).toLong()
        } else {
            null
        }
    }
    
    /**
     * Reset state for new episode
     */
    fun reset() {
        stopMonitoring()
        currentTimestamps = emptyList()
        _skipState.value = SkipOverlayState()
    }
    
    private fun updateSkipState(positionSeconds: Double) {
        val activeTimestamp = currentTimestamps.find { it.isWithinRange(positionSeconds) }
        val upcomingTimestamp = currentTimestamps.find { 
            it.shouldShowSkipButton(positionSeconds, SKIP_BUTTON_LEAD_TIME) && !it.isWithinRange(positionSeconds)
        }
        
        val showButton = activeTimestamp != null || upcomingTimestamp != null
        val currentSegment = activeTimestamp ?: upcomingTimestamp
        
        // Check if we've exited a segment
        val previousSegment = _skipState.value.activeSegment
        if (previousSegment != null && activeTimestamp == null) {
            // Segment ended naturally
            _skipState.value = _skipState.value.copy(
                activeSegment = null,
                showSkipButton = false,
                dismissed = false
            )
            return
        }
        
        // Don't show button if dismissed for this segment
        if (_skipState.value.dismissed && currentSegment == previousSegment) {
            return
        }
        
        // Update state
        if (currentSegment != previousSegment) {
            _skipState.value = _skipState.value.copy(
                activeSegment = currentSegment,
                showSkipButton = showButton && !_skipState.value.dismissed,
                segmentProgress = if (activeTimestamp != null) {
                    ((positionSeconds - activeTimestamp.startTime) / activeTimestamp.durationSeconds).toFloat()
                } else 0f,
                dismissed = false
            )
            
            // Reset dismissed state for new segment
            if (currentSegment != null) {
                startAutoSkipTimer(currentSegment)
            }
        } else if (activeTimestamp != null) {
            // Update progress within segment
            _skipState.value = _skipState.value.copy(
                segmentProgress = ((positionSeconds - activeTimestamp.startTime) / activeTimestamp.durationSeconds).toFloat()
            )
        }
    }
    
    private fun startAutoSkipTimer(segment: SkipTimestamp) {
        if (!autoSkipEnabled) return
        
        val shouldAutoSkip = when (segment.type) {
            SkipType.OPENING -> autoSkipOpenings
            SkipType.ENDING -> autoSkipEndings
            SkipType.RECAP -> autoSkipRecaps
            SkipType.UNKNOWN -> false
        }
        
        if (!shouldAutoSkip) return
        
        autoSkipJob?.cancel()
        // Auto-skip is handled by the player ViewModel listening to state changes
        // This controller just provides the data
    }
}

/**
 * State for skip overlay UI
 */
data class SkipOverlayState(
    val isLoading: Boolean = false,
    val hasTimestamps: Boolean = false,
    val activeSegment: SkipTimestamp? = null,
    val showSkipButton: Boolean = false,
    val segmentProgress: Float = 0f,
    val availableSkips: List<SkipType> = emptyList(),
    val lastSkippedType: SkipType? = null,
    val dismissed: Boolean = false,
    val error: String? = null
) {
    val skipButtonText: String
        get() = activeSegment?.let { "Skip ${it.type.displayName}" } ?: "Skip"
    
    val remainingSeconds: Int
        get() = activeSegment?.let { 
            ((1 - segmentProgress) * it.durationSeconds).toInt()
        } ?: 0
}

/**
 * Skip segment visualization data for progress bar
 */
data class SkipSegmentMarker(
    val type: SkipType,
    val startPercent: Float,
    val endPercent: Float
) {
    companion object {
        fun fromTimestamps(timestamps: List<SkipTimestamp>, totalDuration: Long): List<SkipSegmentMarker> {
            if (totalDuration <= 0) return emptyList()
            
            return timestamps.map { timestamp ->
                SkipSegmentMarker(
                    type = timestamp.type,
                    startPercent = (timestamp.startTime * 1000 / totalDuration).toFloat(),
                    endPercent = (timestamp.endTime * 1000 / totalDuration).toFloat()
                )
            }
        }
    }
}
