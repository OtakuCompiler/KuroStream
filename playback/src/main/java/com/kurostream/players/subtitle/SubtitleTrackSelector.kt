package com.kurostream.players.subtitle

import androidx.media3.common.C
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.exoplayer.ExoPlayer

class SubtitleTrackSelector(private val player: ExoPlayer) {

    fun getSubtitleTracks(): List<SubtitleTrack> {
        return player.currentTracks.groups
            .filter { it.type == C.TRACK_TYPE_TEXT }
            .flatMapIndexed { groupIndex, group ->
                (0 until group.length).map { trackIndex ->
                    val format = group.getTrackFormat(trackIndex)
                    SubtitleTrack(
                        groupIndex = groupIndex,
                        trackIndex = trackIndex,
                        language = format.language ?: "Unknown",
                        label = format.label ?: format.language ?: "Unknown",
                        isSelected = group.isTrackSelected(trackIndex)
                    )
                }
            }
    }

    fun selectSubtitleTrack(groupIndex: Int, trackIndex: Int) {
        val params = player.trackSelectionParameters
            .buildUpon()
            .setOverrideForType(
                TrackSelectionOverride(
                    player.currentTracks.groups[groupIndex].mediaTrackGroup,
                    trackIndex
                )
            )
            .build()
        player.trackSelectionParameters = params
    }

    fun disableSubtitles() {
        val params = player.trackSelectionParameters
            .buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
            .build()
        player.trackSelectionParameters = params
    }

    data class SubtitleTrack(
        val groupIndex: Int,
        val trackIndex: Int,
        val language: String,
        val label: String,
        val isSelected: Boolean
    )
}
