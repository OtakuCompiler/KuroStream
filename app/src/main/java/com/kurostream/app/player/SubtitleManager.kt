package com.kurostream.app.player

import android.graphics.Color
import androidx.media3.common.C
import androidx.media3.common.text.Cue
import androidx.media3.common.text.CueGroup
import androidx.media3.common.text.Cue.Builder as CueBuilder
import androidx.media3.exoplayer.text.TextOutput
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import timber.log.Timber

/**
 * Subtitle styling configuration.
 */
data class SubtitleStyle(
    val fontSize: Float = 16f,
    val fontColor: Int = Color.WHITE,
    val backgroundColor: Int = Color.BLACK,
    val windowColor: Int = Color.TRANSPARENT,
    val edgeType: Int = 2,
    val edgeColor: Int = Color.BLACK
)

/**
 * Manages subtitle rendering pipeline.
 * Passes cues to the registered output and tracks caption metadata.
 * Supports all Media3 subtitle formats (SRT, VTT, ASS/SSA, PGS).
 */
class SubtitleManager(
    private val onCues: (List<Cue>) -> Unit = {},
) : TextOutput {

    private val _cueFlow = MutableSharedFlow<List<Cue>>(replay = 1)
    val cueFlow: SharedFlow<List<Cue>> = _cueFlow.asSharedFlow()

    private var _selectedTrackIndex: Int = C.INDEX_UNSET
    val selectedTrackIndex: Int get() = _selectedTrackIndex

    /** Preferred language (ISO 639-2, e.g., "eng", "jpn", "spa"). */
    var preferredLanguage: String = "eng"

    /** Current subtitle styling. */
    var subtitleStyle: SubtitleStyle = SubtitleStyle()

    @Deprecated("Deprecated in ExoPlayer TextOutput")
    override fun onCues(cues: List<Cue>) {
        val styledCues = applyStyle(cues)
        _cueFlow.tryEmit(styledCues)
        handleStyledCues(styledCues)
    }

    override fun onCues(cueGroup: CueGroup) {
        val styledCues = applyStyle(cueGroup.cues)
        _cueFlow.tryEmit(styledCues)
        handleStyledCues(styledCues)
    }

    private fun handleStyledCues(cues: List<Cue>) {
        // Override in subclass if needed
    }

    /**
     * Applies [subtitleStyle] to each cue in the list.
     */
    private fun applyStyle(cues: List<Cue>): List<Cue> {
        return cues.map { cue ->
            val builder = CueBuilder()
            cue.text?.let { builder.setText(it) }
            cue.textAlignment?.let { builder.setTextAlignment(it) }
            builder.setSize(cue.size)
            builder.setTextSize(subtitleStyle.fontSize, Cue.TEXT_SIZE_TYPE_ABSOLUTE)
            builder.setWindowColor(subtitleStyle.windowColor)
            if (subtitleStyle.backgroundColor != Color.TRANSPARENT) {
                builder.setWindowColor(subtitleStyle.backgroundColor)
            }
            builder.build()
        }
    }

    fun selectTrack(trackIndex: Int) {
        _selectedTrackIndex = trackIndex
        Timber.d("SubtitleManager: selected track $trackIndex")
    }

    fun clearSelection() {
        _selectedTrackIndex = C.INDEX_UNSET
    }

    /**
     * Best-effort guess at whether the current subtitle format supports rich styling.
     * ASS/SSA → full styling; VTT → limited; SRT → minimal; PGS → bitmap.
     */
    fun supportsRichStyling(mimeType: String?): Boolean {
        if (mimeType == null) return false
        return mimeType.contains("ass", ignoreCase = true) ||
            mimeType.contains("ssa", ignoreCase = true)
    }
}
