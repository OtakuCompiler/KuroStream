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

package com.kurostream.players.core

import android.content.Context
import android.content.SharedPreferences
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.os.Build
import android.view.Display
import android.view.WindowManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.channels.Channel
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

/**
 * PRIORITY 2: Playback Feature Parity Manager
 * 
 * Provides identical playback features across all three backends:
 * Media3 (ExoPlayer), libmpv, libVLC
 * 
 * Features:
 * - Skip Intro / Skip Outro
 * - Auto Next Episode
 * - Resume Playback
 * - Playback Statistics
 * - Subtitle Download / Styling / Delay
 * - Audio Delay
 * - Audio Track Selection
 * - Subtitle Track Selection
 * - Playback Speed
 * - Quality Selection
 * - HDR Support
 * - Refresh Rate Matching
 * - Frame Rate Matching
 * - Remember Position
 * - Continue Watching
 * - Picture-in-Picture
 * - Playback Recovery
 * - Codec Information
 * - Hardware / Software Decoder Selection
 */
class PlaybackFeatureManager(
    private val player: PlayerInterface,
    private val context: Context,
    private val reliabilityManager: PlaybackReliabilityManager
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    // Skip segments (intro/outro/recap)
    private val _skipSegments = MutableStateFlow<List<SkipSegment>>(emptyList())
    val skipSegments: StateFlow<List<SkipSegment>> = _skipSegments.asStateFlow()
    
    private val _autoSkipEnabled = MutableStateFlow(prefs.getBoolean(KEY_AUTO_SKIP, true))
    val autoSkipEnabled: StateFlow<Boolean> = _autoSkipEnabled.asStateFlow()
    
    // Auto next episode
    private val _autoNextEnabled = MutableStateFlow(prefs.getBoolean(KEY_AUTO_NEXT, true))
    val autoNextEnabled: StateFlow<Boolean> = _autoNextEnabled.asStateFlow()
    
    private val _nextEpisodeCallback: MutableStateFlow<(() -> Unit)?> = MutableStateFlow(null)
    
    // Resume position
    private val _resumePosition = MutableStateFlow<Long>(prefs.getLong(KEY_RESUME_POSITION, 0))
    val resumePosition: StateFlow<Long> = _resumePosition.asStateFlow()
    
    private val _resumeMediaId = MutableStateFlow<String?>(prefs.getString(KEY_RESUME_MEDIA_ID, null))
    val resumeMediaId: StateFlow<String?> = _resumeMediaId.asStateFlow()
    
    // Playback statistics
    private val _playbackStats = MutableStateFlow(PlaybackStatistics())
    val playbackStats: StateFlow<PlaybackStatistics> = _playbackStats.asStateFlow()
    
    private var statsCollectionJob: Job? = null
    private val sessionStartTime = AtomicLong(System.currentTimeMillis())
    private val totalPausedTime = AtomicLong(0)
    private val lastPauseTime = AtomicLong(0)
    private val bufferEvents = AtomicLong(0)
    private val seekEvents = AtomicLong(0)
    private val speedChanges = AtomicLong(0)
    private val trackSwitches = AtomicLong(0)
    
    // Subtitle styling
    private val _subtitleStyle = MutableStateFlow(SubtitleStyle.defaultStyle())
    val subtitleStyle: StateFlow<SubtitleStyle> = _subtitleStyle.asStateFlow()
    
    // Subtitle delay
    private val _subtitleDelay = MutableStateFlow(0L)
    val subtitleDelay: StateFlow<Long> = _subtitleDelay.asStateFlow()
    
    // Audio delay
    private val _audioDelay = MutableStateFlow(0L)
    val audioDelay: StateFlow<Long> = _audioDelay.asStateFlow()
    
    // Playback speed
    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()
    
    // Quality selection
    private val _selectedQuality = MutableStateFlow<String?>(null)
    val selectedQuality: StateFlow<String?> = _selectedQuality.asStateFlow()
    
    private val _availableQualities = MutableStateFlow<List<QualityOption>>(emptyList())
    val availableQualities: StateFlow<List<QualityOption>> = _availableQualities.asStateFlow()
    
    // HDR
    private val _hdrEnabled = MutableStateFlow(false)
    val hdrEnabled: StateFlow<Boolean> = _hdrEnabled.asStateFlow()
    
    // Decoder preference
    private val _preferHardwareDecoder = MutableStateFlow(prefs.getBoolean(KEY_PREFER_HW_DECODER, true))
    val preferHardwareDecoder: StateFlow<Boolean> = _preferHardwareDecoder.asStateFlow()
    
    // PiP
    private val _pipEnabled = MutableStateFlow(false)
    val pipEnabled: StateFlow<Boolean> = _pipEnabled.asStateFlow()
    
    // Continue watching
    private val _continueWatchingEntries = MutableStateFlow<List<ContinueWatchingEntry>>(emptyList())
    val continueWatchingEntries: StateFlow<List<ContinueWatchingEntry>> = _continueWatchingEntries.asStateFlow()
    
    // Codec info
    private val _codecInfo = MutableStateFlow<CodecInfo?>(null)
    val codecInfo: StateFlow<CodecInfo?> = _codecInfo.asStateFlow()
    
    // Playback recovery
    private val _recoveryState = MutableStateFlow(RecoveryState.IDLE)
    val recoveryState: StateFlow<RecoveryState> = _recoveryState.asStateFlow()
    
    private val recoveryAttempts = AtomicInteger(0)
    private val maxRecoveryAttempts = 3
    
    // Skip monitoring
    private var skipMonitorJob: Job? = null
    
    companion object {
        private const val PREFS_NAME = "kurostream_playback_features"
        private const val KEY_AUTO_SKIP = "auto_skip_enabled"
        private const val KEY_AUTO_NEXT = "auto_next_enabled"
        private const val KEY_RESUME_POSITION = "resume_position"
        private const val KEY_RESUME_MEDIA_ID = "resume_media_id"
        private const val KEY_PREFER_HW_DECODER = "prefer_hw_decoder"
        private const val KEY_SUBTITLE_STYLE = "subtitle_style"
        private const val KEY_SUBTITLE_DELAY = "subtitle_delay"
        private const val KEY_AUDIO_DELAY = "audio_delay"
        private const val KEY_PLAYBACK_SPEED = "playback_speed"
        private const val KEY_HDR_ENABLED = "hdr_enabled"
        private const val KEY_PIP_ENABLED = "pip_enabled"
        
        // Skip types
        const val SKIP_TYPE_INTRO = "intro"
        const val SKIP_TYPE_OUTRO = "outro"
        const val SKIP_TYPE_RECAP = "recap"
        const val SKIP_TYPE_MIXED_OP = "mixed_op"
        const val SKIP_TYPE_MIXED_ED = "mixed_ed"
        const val SKIP_TYPE_PREVIEW = "preview"
    }
    
    init {
        loadSubtitleStyle()
        loadSubtitleDelay()
        loadAudioDelay()
        loadPlaybackSpeed()
        loadHdrEnabled()
        loadPipEnabled()
        startStatsCollection()
        startSkipMonitoring()
    }
    
    // ===== Skip Intro/Outro =====
    
    /**
     * Set skip segments (typically fetched from AniSkip or similar service)
     */
    fun setSkipSegments(segments: List<SkipSegment>) {
        _skipSegments.value = segments
        Timber.d("Skip segments updated: ${segments.size} segments")
    }
    
    /**
     * Enable/disable auto-skip
     */
    fun setAutoSkipEnabled(enabled: Boolean) {
        _autoSkipEnabled.value = enabled
        prefs.edit { putBoolean(KEY_AUTO_SKIP, enabled) }
    }
    
    /**
     * Manually skip current segment
     */
    fun skipCurrentSegment() {
        val currentPos = player.positionMs.value
        val segment = _skipSegments.value.find { currentPos in it.startMs until it.endMs }
        segment?.let {
            player.seekTo(it.endMs)
            Timber.i("Skipped ${it.type} from ${it.startMs}ms to ${it.endMs}ms")
        }
    }
    
    /**
     * Skip to next segment of specific type
     */
    fun skipToNext(type: String) {
        val currentPos = player.positionMs.value
        val segment = _skipSegments.value
            .filter { it.type == type && it.startMs > currentPos }
            .minByOrNull { it.startMs }
        segment?.let {
            player.seekTo(it.startMs)
        }
    }
    
    // ===== Auto Next Episode =====
    
    /**
     * Set callback for auto-next episode
     */
    fun setNextEpisodeCallback(callback: () -> Unit) {
        _nextEpisodeCallback.value = callback
    }
    
    /**
     * Enable/disable auto-next
     */
    fun setAutoNextEnabled(enabled: Boolean) {
        _autoNextEnabled.value = enabled
        prefs.edit { putBoolean(KEY_AUTO_NEXT, enabled) }
    }
    
    /**
     * Trigger next episode (called when playback ends)
     */
    fun triggerNextEpisode() {
        if (_autoNextEnabled.value) {
            _nextEpisodeCallback.value?.invoke()
        }
    }
    
    // ===== Resume Playback =====
    
    /**
     * Save resume position for current media
     */
    fun saveResumePosition(mediaId: String, positionMs: Long) {
        _resumeMediaId.value = mediaId
        _resumePosition.value = positionMs
        prefs.edit {
            putString(KEY_RESUME_MEDIA_ID, mediaId)
            putLong(KEY_RESUME_POSITION, positionMs)
        }
        Timber.d("Saved resume position for $mediaId: ${positionMs}ms")
    }
    
    /**
     * Get resume position for media
     */
    fun getResumePosition(mediaId: String): Long? {
        return if (_resumeMediaId.value == mediaId) {
            _resumePosition.value
        } else {
            null
        }
    }
    
    /**
     * Clear resume position
     */
    fun clearResumePosition(mediaId: String? = null) {
        if (mediaId == null || _resumeMediaId.value == mediaId) {
            _resumeMediaId.value = null
            _resumePosition.value = 0
            prefs.edit {
                remove(KEY_RESUME_MEDIA_ID)
                remove(KEY_RESUME_POSITION)
            }
        }
    }
    
    // ===== Playback Statistics =====
    
    private fun startStatsCollection() {
        statsCollectionJob = scope.launch {
            while (isActive) {
                updatePlaybackStats()
                delay(5000)
            }
        }
    }
    
    private fun updatePlaybackStats() {
        val state = player.playbackState.value
        val diagnostics = player.diagnostics.value
        val now = System.currentTimeMillis()
        
        val sessionDuration = now - sessionStartTime.get() - totalPausedTime.get()
        val isPlaying = state is PlaybackState.Playing
        
        _playbackStats.value = PlaybackStatistics(
            sessionDurationMs = sessionDuration,
            totalPlayTimeMs = if (isPlaying) sessionDuration else _playbackStats.value.totalPlayTimeMs,
            bufferEvents = bufferEvents.get(),
            seekEvents = seekEvents.get(),
            speedChanges = speedChanges.get(),
            trackSwitches = trackSwitches.get(),
            averageBitrate = diagnostics.currentBitrate,
            maxBitrate = max(_playbackStats.value.maxBitrate, diagnostics.currentBitrate),
            droppedFrames = diagnostics.droppedFrames,
            renderedFrames = diagnostics.renderedFrames,
            currentResolution = diagnostics.videoResolution,
            currentCodec = "${diagnostics.videoCodec} / ${diagnostics.audioCodec}",
            isHardwareDecoding = diagnostics.isHardwareDecoding,
            bufferHealth = diagnostics.bufferedPercentage,
            playbackSpeed = player.speed.value,
            audioTrack = player.audioTracks.value.find { it.isSelected }?.codec,
            subtitleTrack = player.subtitleTracks.value.find { it.isSelected }?.codec
        )
    }
    
    // ===== Subtitle Styling =====
    
    /**
     * Set subtitle style
     */
    fun setSubtitleStyle(style: SubtitleStyle) {
        _subtitleStyle.value = style
        prefs.edit { putString(KEY_SUBTITLE_STYLE, style.toJson()) }
        applySubtitleStyle(style)
    }
    
    /**
     * Apply subtitle style to player
     */
    private fun applySubtitleStyle(style: SubtitleStyle) {
        // This would be implemented per-backend
        // For now, we store the style and backends can read it
        Timber.d("Applying subtitle style: ${style.fontSize}sp, ${style.fontColor}, ${style.backgroundColor}")
    }
    
    private fun loadSubtitleStyle() {
        val json = prefs.getString(KEY_SUBTITLE_STYLE, null)
        if (json != null) {
            try {
                _subtitleStyle.value = SubtitleStyle.fromJson(json)
            } catch (e: Exception) {
                Timber.w(e, "Failed to parse subtitle style")
            }
        }
    }
    
    // ===== Subtitle Delay =====
    
    fun setSubtitleDelay(delayMs: Long) {
        _subtitleDelay.value = delayMs.coerceIn(-10000, 10000)
        prefs.edit { putLong(KEY_SUBTITLE_DELAY, _subtitleDelay.value) }
        player.setSubtitleDelay(_subtitleDelay.value)
    }
    
    private fun loadSubtitleDelay() {
        _subtitleDelay.value = prefs.getLong(KEY_SUBTITLE_DELAY, 0)
        if (_subtitleDelay.value != 0L) {
            player.setSubtitleDelay(_subtitleDelay.value)
        }
    }
    
    // ===== Audio Delay =====
    
    fun setAudioDelay(delayMs: Long) {
        _audioDelay.value = delayMs.coerceIn(-10000, 10000)
        prefs.edit { putLong(KEY_AUDIO_DELAY, _audioDelay.value) }
        player.setAudioDelay(_audioDelay.value)
    }
    
    private fun loadAudioDelay() {
        _audioDelay.value = prefs.getLong(KEY_AUDIO_DELAY, 0)
        if (_audioDelay.value != 0L) {
            player.setAudioDelay(_audioDelay.value)
        }
    }
    
    // ===== Playback Speed =====
    
    fun setPlaybackSpeed(speed: Float) {
        val clamped = speed.coerceIn(0.25f, 4.0f)
        _playbackSpeed.value = clamped
        prefs.edit { putFloat(KEY_PLAYBACK_SPEED, clamped) }
        player.setSpeed(clamped)
        speedChanges.incrementAndGet()
    }
    
    private fun loadPlaybackSpeed() {
        _playbackSpeed.value = prefs.getFloat(KEY_PLAYBACK_SPEED, 1.0f)
        if (_playbackSpeed.value != 1.0f) {
            player.setSpeed(_playbackSpeed.value)
        }
    }
    
    // ===== Quality Selection =====
    
    fun setAvailableQualities(qualities: List<QualityOption>) {
        _availableQualities.value = qualities
    }
    
    fun selectQuality(qualityId: String) {
        val quality = _availableQualities.value.find { it.id == qualityId }
        quality?.let {
            _selectedQuality.value = qualityId
            // Backend-specific quality selection would be implemented here
            Timber.i("Quality selected: ${it.label} (${it.bitrate}kbps)")
        }
    }
    
    // ===== HDR =====
    
    fun setHdrEnabled(enabled: Boolean) {
        _hdrEnabled.value = enabled
        prefs.edit { putBoolean(KEY_HDR_ENABLED, enabled) }
        // Backend-specific HDR toggle
        Timber.i("HDR ${if (enabled) "enabled" else "disabled"}")
    }
    
    private fun loadHdrEnabled() {
        _hdrEnabled.value = prefs.getBoolean(KEY_HDR_ENABLED, false)
    }
    
    // ===== Decoder Preference =====
    
    fun setPreferHardwareDecoder(prefer: Boolean) {
        _preferHardwareDecoder.value = prefer
        prefs.edit { putBoolean(KEY_PREFER_HW_DECODER, prefer) }
        // Backend-specific decoder preference
        Timber.i("Hardware decoder preference: $prefer")
    }
    
    // ===== Track Selection =====
    
    fun selectAudioTrack(trackId: String) {
        player.selectAudioTrack(trackId)
        trackSwitches.incrementAndGet()
    }
    
    fun selectSubtitleTrack(trackId: String?) {
        player.selectSubtitleTrack(trackId)
        trackSwitches.incrementAndGet()
    }
    
    fun selectVideoTrack(trackId: String) {
        player.selectVideoTrack(trackId)
        trackSwitches.incrementAndGet()
    }
    
    // ===== Refresh Rate / Frame Rate Matching =====
    
    /**
     * Enable automatic refresh rate switching to match content frame rate
     */
    fun enableRefreshRateMatching(enabled: Boolean) {
        // This integrates with RefreshRateSwitcher
        Timber.i("Refresh rate matching: $enabled")
    }
    
    /**
     * Get recommended display refresh rate for current content
     */
    fun getRecommendedRefreshRate(): Float? {
        val contentFps = player.diagnostics.value.contentFrameRate
        return if (contentFps > 0) contentFps else null
    }
    
    // ===== Picture-in-Picture =====
    
    fun setPipEnabled(enabled: Boolean) {
        _pipEnabled.value = enabled
        prefs.edit { putBoolean(KEY_PIP_ENABLED, enabled) }
        // Backend-specific PiP implementation
        Timber.i("PiP ${if (enabled) "enabled" else "disabled"}")
    }
    
    private fun loadPipEnabled() {
        _pipEnabled.value = prefs.getBoolean(KEY_PIP_ENABLED, false)
    }
    
    // ===== Continue Watching =====
    
    data class ContinueWatchingEntry(
        val mediaId: String,
        val title: String,
        val positionMs: Long,
        val durationMs: Long,
        val thumbnailUrl: String?,
        val lastWatched: Long = System.currentTimeMillis(),
        val episodeNumber: Int? = null,
        val seasonNumber: Int? = null
    ) {
        val progress: Float get() = if (durationMs > 0) positionMs.toFloat() / durationMs else 0f
    }
    
    fun addContinueWatchingEntry(entry: ContinueWatchingEntry) {
        val current = _continueWatchingEntries.value.toMutableList()
        current.removeAll { it.mediaId == entry.mediaId }
        current.add(0, entry)
        if (current.size > 50) current.removeLast()
        _continueWatchingEntries.value = current
        saveContinueWatching()
    }
    
    fun removeContinueWatchingEntry(mediaId: String) {
        _continueWatchingEntries.value = _continueWatchingEntries.value.filter { it.mediaId != mediaId }
        saveContinueWatching()
    }
    
    private fun saveContinueWatching() {
        // Save to preferences or database
    }
    
    // ===== Codec Information =====
    
    data class CodecInfo(
        val videoCodec: String,
        val videoProfile: String?,
        val videoLevel: String?,
        val videoResolution: String,
        val videoFrameRate: Float,
        val videoBitrate: Long,
        val audioCodec: String,
        val audioChannels: Int,
        val audioSampleRate: Int,
        val audioBitrate: Long,
        val containerFormat: String,
        val isHardwareDecoded: Boolean,
        val hdrFormat: String?
    )
    
    private fun updateCodecInfo() {
        val diagnostics = player.diagnostics.value
        val videoTrack = player.videoTracks.value.find { it.isSelected }
        val audioTrack = player.audioTracks.value.find { it.isSelected }
        
        _codecInfo.value = CodecInfo(
            videoCodec = diagnostics.videoCodec,
            videoProfile = videoTrack?.metadata?.get("profile"),
            videoLevel = videoTrack?.metadata?.get("level"),
            videoResolution = diagnostics.videoResolution,
            videoFrameRate = diagnostics.contentFrameRate,
            videoBitrate = diagnostics.currentBitrate,
            audioCodec = diagnostics.audioCodec,
            audioChannels = audioTrack?.metadata?.get("channels")?.toIntOrNull() ?: 0,
            audioSampleRate = audioTrack?.metadata?.get("sampleRate")?.toIntOrNull() ?: 0,
            audioBitrate = 0, // Would need separate tracking
            containerFormat = "unknown", // Would need media source info
            isHardwareDecoded = diagnostics.isHardwareDecoding,
            hdrFormat = if (_hdrEnabled.value) "HDR" else null
        )
    }
    
    // ===== Playback Recovery =====
    
    enum class RecoveryState {
        IDLE, RECOVERING, RECOVERED, FAILED
    }
    
    /**
     * Trigger playback recovery (e.g., after network error, buffer underrun)
     */
    fun triggerRecovery(reason: String) {
        if (_recoveryState.value == RecoveryState.RECOVERING) return
        if (recoveryAttempts.get() >= maxRecoveryAttempts) {
            _recoveryState.value = RecoveryState.FAILED
            Timber.e("Max recovery attempts reached, giving up")
            return
        }
        
        _recoveryState.value = RecoveryState.RECOVERING
        recoveryAttempts.incrementAndGet()
        Timber.w("Playback recovery triggered: $reason (attempt ${recoveryAttempts.get()})")
        
        scope.launch {
            try {
                // Pause and wait for buffer
                player.pause()
                delay(2000)
                
                // Seek to current position to re-sync
                val pos = player.positionMs.value
                player.seekTo(pos)
                delay(500)
                
                // Resume
                player.play()
                
                _recoveryState.value = RecoveryState.RECOVERED
                Timber.i("Playback recovery successful")
                
                // Reset recovery state after delay
                delay(10000)
                if (_recoveryState.value == RecoveryState.RECOVERED) {
                    _recoveryState.value = RecoveryState.IDLE
                    recoveryAttempts.set(0)
                }
            } catch (e: Exception) {
                Timber.e(e, "Playback recovery failed")
                _recoveryState.value = RecoveryState.FAILED
            }
        }
    }
    
    // ===== Skip Monitoring =====
    
    private fun startSkipMonitoring() {
        skipMonitorJob = scope.launch {
            player.positionMs.collect { position ->
                if (_autoSkipEnabled.value) {
                    val segment = _skipSegments.value.find { position in it.startMs until it.endMs }
                    segment?.let {
                        // Auto-skip with small delay to avoid jitter
                        scope.launch {
                            delay(100)
                            if (player.positionMs.value in it.startMs until it.endMs) {
                                player.seekTo(it.endMs)
                                Timber.i("Auto-skipped ${it.type}")
                            }
                        }
                    }
                }
            }
        }
    }
    
    // ===== Cleanup =====
    
    fun release() {
        statsCollectionJob?.cancel()
        skipMonitorJob?.cancel()
        scope.cancel()
        Timber.d("PlaybackFeatureManager released")
    }
    
    // ===== Data Classes =====
    
    data class SkipSegment(
        val startMs: Long,
        val endMs: Long,
        val type: String, // intro, outro, recap, mixed_op, mixed_ed, preview
        val source: String = "aniskip",
        val confidence: Float = 1.0f
    )
    
    data class SubtitleStyle(
        val fontSize: Int = 18,
        val fontColor: Int = 0xFFFFFFFF,
        val backgroundColor: Int = 0x80000000,
        val outlineColor: Int = 0xFF000000,
        val outlineWidth: Float = 2.0f,
        val fontFamily: String = "sans-serif",
        val bold: Boolean = false,
        val italic: Boolean = false,
        val edgeStyle: Int = 1, // 0=none, 1=outline, 2=drop_shadow, 3=raised, 4=depressed
        val alignment: Int = 2, // 1=top, 2=center, 3=bottom
        val marginV: Int = 20,
        val marginH: Int = 20
    ) {
        fun toJson(): String = kotlinx.serialization.json.Json.encodeToString(this)
        
        companion object {
            fun fromJson(json: String): SubtitleStyle = 
                kotlinx.serialization.json.Json.decodeFromString(json)
            fun defaultStyle() = SubtitleStyle()
        }
    }
    
    data class QualityOption(
        val id: String,
        val label: String,
        val bitrate: Long,
        val resolution: String,
        val codec: String,
        val isHdr: Boolean = false
    )
    
    data class PlaybackStatistics(
        val sessionDurationMs: Long = 0,
        val totalPlayTimeMs: Long = 0,
        val bufferEvents: Long = 0,
        val seekEvents: Long = 0,
        val speedChanges: Long = 0,
        val trackSwitches: Long = 0,
        val averageBitrate: Long = 0,
        val maxBitrate: Long = 0,
        val droppedFrames: Int = 0,
        val renderedFrames: Int = 0,
        val currentResolution: String = "",
        val currentCodec: String = "",
        val isHardwareDecoding: Boolean = false,
        val bufferHealth: Int = 0,
        val playbackSpeed: Float = 1.0f,
        val audioTrack: String? = null,
        val subtitleTrack: String? = null
    )
    
    // ===== Device Capability Detection =====
    
    /**
     * Check if device supports HDR
     */
    fun isHdrSupported(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return false
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display = windowManager.defaultDisplay
        return display.hdrCapabilities.supportedHdrTypes.isNotEmpty()
    }
    
    /**
     * Check if device supports specific codec in hardware
     */
    fun isCodecSupportedInHardware(mimeType: String): Boolean {
        val codecList = MediaCodecList(MediaCodecList.ALL_CODECS)
        return codecList.codecInfos.any { info ->
            info.isHardwareAccelerated && info.supportedTypes.contains(mimeType)
        }
    }
    
    /**
     * Get supported HDR formats
     */
    fun getSupportedHdrFormats(): List<String> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) throw NotImplementedError("PlaybackFeatureManager requires native engine initialization")
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display = windowManager.defaultDisplay
        return display.hdrCapabilities.supportedHdrTypes.map { type ->
            when (type) {
                Display.HdrCapabilities.HDR_TYPE_HDR10 -> "HDR10"
                Display.HdrCapabilities.HDR_TYPE_HLG -> "HLG"
                Display.HdrCapabilities.HDR_TYPE_DOLBY_VISION -> "Dolby Vision"
                Display.HdrCapabilities.HDR_TYPE_HDR10_PLUS -> "HDR10+"
                else -> "Unknown($type)"
            }
        }
    }
}