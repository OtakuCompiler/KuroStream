package com.kurostream.players.video

import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector

class QualitySelector(private val player: ExoPlayer) {

    fun getAvailableQualities(): List<VideoQuality> {
        return player.currentTracks.groups
            .filter { it.type == C.TRACK_TYPE_VIDEO }
            .flatMap { group ->
                (0 until group.length).map { trackIndex ->
                    val format = group.getTrackFormat(trackIndex)
                    VideoQuality(
                        width = format.width,
                        height = format.height,
                        bitrate = format.bitrate,
                        isSelected = group.isTrackSelected(trackIndex)
                    )
                }
            }
            .distinctBy { it.height }
            .sortedByDescending { it.height }
    }

    fun setQuality(height: Int?) {
        val trackSelector = (player.trackSelector as? DefaultTrackSelector) ?: return
        val params = if (height == null) {
            trackSelector.buildUponParameters().clearOverridesOfType(C.TRACK_TYPE_VIDEO)
        } else {
            trackSelector.buildUponParameters().setMaxVideoSize(height, Int.MAX_VALUE)
        }
        trackSelector.setParameters(params)
    }

    data class VideoQuality(
        val width: Int,
        val height: Int,
        val bitrate: Int,
        val isSelected: Boolean
    ) {
        val label: String get() = when {
            height >= 2160 -> "4K"
            height >= 1440 -> "1440p"
            height >= 1080 -> "1080p"
            height >= 720 -> "720p"
            height >= 480 -> "480p"
            else -> "${height}p"
        }
    }
}
