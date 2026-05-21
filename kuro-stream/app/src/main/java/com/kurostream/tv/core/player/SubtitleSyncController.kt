package com.kurostream.tv.core.player

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import androidx.media3.common.TrackGroup
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Controller for managing subtitle synchronization and selection
 * Provides real-time subtitle offset adjustment and track management
 */
@Singleton
class SubtitleSyncController @Inject constructor() {
    
    companion object {
        private const val TAG = "SubtitleSyncController"
        private const val MAX_OFFSET_MS = 10000L // 10 seconds max offset
        private const val MIN_OFFSET_MS = -10000L
        private const val DEFAULT_STEP_MS = 100L // 100ms adjustment step
        private const val LARGE_STEP_MS = 500L // 500ms for larger adjustments
    }
    
    private val _subtitleState = MutableStateFlow(SubtitleState())
    val subtitleState: StateFlow<SubtitleState> = _subtitleState.asStateFlow()
    
    private var currentOffset: Long = 0L
    private var player: ExoPlayer? = null
    
    /**
     * Attach to an ExoPlayer instance
     */
    @OptIn(UnstableApi::class)
    fun attachPlayer(exoPlayer: ExoPlayer) {
        this.player = exoPlayer
        updateAvailableTracks()
    }
    
    /**
     * Detach from player
     */
    fun detachPlayer() {
        this.player = null
        _subtitleState.value = SubtitleState()
    }
    
    /**
     * Update available subtitle tracks from player
     */
    @OptIn(UnstableApi::class)
    fun updateAvailableTracks() {
        val player = this.player ?: return
        
        val tracks = mutableListOf<SubtitleTrack>()
        val currentTracks = player.currentTracks
        
        for (group in currentTracks.groups) {
            if (group.type != C.TRACK_TYPE_TEXT) continue
            
            for (i in 0 until group.length) {
                val format = group.getTrackFormat(i)
                val isSelected = group.isTrackSelected(i)
                
                tracks.add(
                    SubtitleTrack(
                        id = "${group.hashCode()}_$i",
                        language = format.language ?: "Unknown",
                        label = format.label ?: getLanguageLabel(format.language),
                        mimeType = format.sampleMimeType ?: MimeTypes.TEXT_VTT,
                        isSelected = isSelected,
                        isExternal = format.id?.startsWith("external_") == true,
                        format = format,
                        groupIndex = currentTracks.groups.indexOf(group),
                        trackIndex = i
                    )
                )
            }
        }
        
        _subtitleState.value = _subtitleState.value.copy(
            availableTracks = tracks,
            selectedTrack = tracks.find { it.isSelected },
            hasSubtitles = tracks.isNotEmpty()
        )
        
        Timber.tag(TAG).d("Found ${tracks.size} subtitle tracks")
    }
    
    /**
     * Select a subtitle track
     */
    @OptIn(UnstableApi::class)
    fun selectTrack(track: SubtitleTrack?) {
        val player = this.player ?: return
        
        val trackSelector = player.trackSelector ?: return
        val currentTracks = player.currentTracks
        
        val params = trackSelector.parameters.buildUpon()
        
        if (track == null) {
            // Disable subtitles
            params.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
            Timber.tag(TAG).d("Subtitles disabled")
        } else {
            params.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
            
            // Find the track group and set override
            if (track.groupIndex >= 0 && track.groupIndex < currentTracks.groups.size) {
                val group = currentTracks.groups[track.groupIndex]
                val mediaTrackGroup = group.mediaTrackGroup
                
                params.addOverride(
                    TrackSelectionOverride(
                        mediaTrackGroup,
                        listOf(track.trackIndex)
                    )
                )
                Timber.tag(TAG).d("Selected subtitle track: ${track.label}")
            }
        }
        
        trackSelector.parameters = params.build()
        
        _subtitleState.value = _subtitleState.value.copy(
            selectedTrack = track,
            isEnabled = track != null
        )
    }
    
    /**
     * Adjust subtitle offset by a step
     * @param forward True to delay subtitles, false to advance them
     * @param largeStep True for 500ms step, false for 100ms step
     */
    fun adjustOffset(forward: Boolean, largeStep: Boolean = false) {
        val step = if (largeStep) LARGE_STEP_MS else DEFAULT_STEP_MS
        val delta = if (forward) step else -step
        
        setOffset(currentOffset + delta)
    }
    
    /**
     * Set exact subtitle offset
     * @param offsetMs Offset in milliseconds (positive = delay, negative = advance)
     */
    fun setOffset(offsetMs: Long) {
        currentOffset = offsetMs.coerceIn(MIN_OFFSET_MS, MAX_OFFSET_MS)
        
        _subtitleState.value = _subtitleState.value.copy(
            currentOffsetMs = currentOffset
        )
        
        // Apply offset to player
        applyOffsetToPlayer()
        
        Timber.tag(TAG).d("Subtitle offset set to ${currentOffset}ms")
    }
    
    /**
     * Reset subtitle offset to zero
     */
    fun resetOffset() {
        setOffset(0L)
    }
    
    /**
     * Get formatted offset string for display
     */
    fun getFormattedOffset(): String {
        val absOffset = kotlin.math.abs(currentOffset)
        val sign = when {
            currentOffset > 0 -> "+"
            currentOffset < 0 -> "-"
            else -> ""
        }
        return "$sign${absOffset}ms"
    }
    
    /**
     * Toggle subtitles on/off
     */
    fun toggleSubtitles() {
        val currentState = _subtitleState.value
        
        if (currentState.isEnabled) {
            // Disable - remember the current track
            _subtitleState.value = currentState.copy(
                previousTrack = currentState.selectedTrack
            )
            selectTrack(null)
        } else {
            // Enable - restore previous track or select first available
            val trackToSelect = currentState.previousTrack 
                ?: currentState.availableTracks.firstOrNull()
            selectTrack(trackToSelect)
        }
    }
    
    /**
     * Set subtitle text size scale
     * @param scale 0.5 to 2.0
     */
    fun setTextScale(scale: Float) {
        val clampedScale = scale.coerceIn(0.5f, 2.0f)
        _subtitleState.value = _subtitleState.value.copy(
            textScale = clampedScale
        )
        // Note: Text scale needs to be applied via SubtitleView styling
    }
    
    /**
     * Set subtitle background opacity
     * @param opacity 0.0 to 1.0
     */
    fun setBackgroundOpacity(opacity: Float) {
        val clampedOpacity = opacity.coerceIn(0f, 1f)
        _subtitleState.value = _subtitleState.value.copy(
            backgroundOpacity = clampedOpacity
        )
    }
    
    @OptIn(UnstableApi::class)
    private fun applyOffsetToPlayer() {
        // ExoPlayer doesn't have built-in subtitle offset support
        // This would need to be implemented via a custom SubtitleDecoder
        // or by adjusting cue timing in a custom TextRenderer
        // For now, we track the offset and can apply it at the UI layer
        
        // Alternative: Use SubtitleView with adjusted timing
        Timber.tag(TAG).d("Offset tracking: ${currentOffset}ms (UI-level adjustment)")
    }
    
    private fun getLanguageLabel(languageCode: String?): String {
        return when (languageCode?.lowercase()) {
            "en", "eng" -> "English"
            "ja", "jpn" -> "Japanese"
            "es", "spa" -> "Spanish"
            "fr", "fra" -> "French"
            "de", "deu" -> "German"
            "it", "ita" -> "Italian"
            "pt", "por" -> "Portuguese"
            "ru", "rus" -> "Russian"
            "zh", "chi", "zho" -> "Chinese"
            "ko", "kor" -> "Korean"
            "ar", "ara" -> "Arabic"
            "hi", "hin" -> "Hindi"
            "th", "tha" -> "Thai"
            "vi", "vie" -> "Vietnamese"
            "id", "ind" -> "Indonesian"
            "ms", "msa" -> "Malay"
            "pl", "pol" -> "Polish"
            "tr", "tur" -> "Turkish"
            "nl", "nld" -> "Dutch"
            "sv", "swe" -> "Swedish"
            "fi", "fin" -> "Finnish"
            "no", "nor" -> "Norwegian"
            "da", "dan" -> "Danish"
            "cs", "ces" -> "Czech"
            "hu", "hun" -> "Hungarian"
            "ro", "ron" -> "Romanian"
            "el", "ell" -> "Greek"
            "he", "heb" -> "Hebrew"
            "uk", "ukr" -> "Ukrainian"
            "bg", "bul" -> "Bulgarian"
            "hr", "hrv" -> "Croatian"
            "sk", "slk" -> "Slovak"
            "sl", "slv" -> "Slovenian"
            null -> "Unknown"
            else -> languageCode.uppercase()
        }
    }
}

/**
 * Represents a subtitle track
 */
@OptIn(UnstableApi::class)
data class SubtitleTrack(
    val id: String,
    val language: String,
    val label: String,
    val mimeType: String,
    val isSelected: Boolean,
    val isExternal: Boolean,
    val format: Format,
    val groupIndex: Int,
    val trackIndex: Int
) {
    val displayName: String
        get() = if (isExternal) "$label (External)" else label
}

/**
 * Subtitle state for UI
 */
data class SubtitleState(
    val isEnabled: Boolean = false,
    val hasSubtitles: Boolean = false,
    val availableTracks: List<SubtitleTrack> = emptyList(),
    val selectedTrack: SubtitleTrack? = null,
    val previousTrack: SubtitleTrack? = null,
    val currentOffsetMs: Long = 0L,
    val textScale: Float = 1.0f,
    val backgroundOpacity: Float = 0.8f
) {
    val offsetFormatted: String
        get() {
            val absOffset = kotlin.math.abs(currentOffsetMs)
            val sign = when {
                currentOffsetMs > 0 -> "+"
                currentOffsetMs < 0 -> "-"
                else -> ""
            }
            return "$sign${absOffset}ms"
        }
    
    val trackCount: Int get() = availableTracks.size
}

/**
 * Subtitle styling options
 */
data class SubtitleStyle(
    val textColor: Int = 0xFFFFFFFF.toInt(),
    val backgroundColor: Int = 0xCC000000.toInt(),
    val windowColor: Int = 0x00000000,
    val edgeType: Int = 0, // 0=none, 1=outline, 2=drop shadow, 3=raised, 4=depressed
    val edgeColor: Int = 0xFF000000.toInt(),
    val textSizePercent: Int = 100,
    val fontFamily: String = "sans-serif",
    val isBold: Boolean = false,
    val isItalic: Boolean = false
)
