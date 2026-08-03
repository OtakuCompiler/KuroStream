package com.kurostream.players.audio

import android.content.Context
import android.media.AudioFormat
import android.media.AudioManager
import android.os.Build
import androidx.media3.common.C
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import timber.log.Timber

class AudioTrackSelector(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    fun isDolbyAtmosSupported(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return false
        return audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).any { device ->
            device.type == android.media.AudioDeviceInfo.TYPE_HDMI &&
            device.encodings.contains(AudioFormat.ENCODING_E_AC3_JOC)
        }
    }

    fun isDolbyDigitalPlusSupported(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return false
        return audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).any { device ->
            device.encodings.contains(AudioFormat.ENCODING_E_AC3) ||
            device.encodings.contains(AudioFormat.ENCODING_E_AC3_JOC)
        }
    }

    fun configureTrackSelector(trackSelector: DefaultTrackSelector) {
        val params = trackSelector.buildUponParameters()
        if (isDolbyAtmosSupported()) {
            params.setPreferredAudioMimeTypes("audio/eac3-joc", "audio/eac3", "audio/ac3")
            Timber.d("Audio: Dolby Atmos passthrough enabled")
        } else if (isDolbyDigitalPlusSupported()) {
            params.setPreferredAudioMimeTypes("audio/eac3", "audio/ac3")
            Timber.d("Audio: Dolby Digital+ passthrough enabled")
        } else {
            params.setPreferredAudioMimeTypes("audio/mp4a-latm", "audio/opus")
            Timber.d("Audio: Stereo fallback")
        }
        trackSelector.setParameters(params)
    }

    fun getAudioTracks(player: ExoPlayer): List<AudioTrackInfo> {
        return player.currentTracks.groups
            .filter { it.type == C.TRACK_TYPE_AUDIO }
            .flatMapIndexed { groupIndex, group ->
                (0 until group.length).map { trackIndex ->
                    val format = group.getTrackFormat(trackIndex)
                    AudioTrackInfo(
                        groupIndex = groupIndex,
                        trackIndex = trackIndex,
                        language = format.language ?: "Unknown",
                        label = format.label ?: "${format.sampleMimeType}",
                        isSelected = group.isTrackSelected(trackIndex)
                    )
                }
            }
    }

    fun selectAudioTrack(player: ExoPlayer, groupIndex: Int, trackIndex: Int) {
        val trackSelector = player.trackSelectionParameters
            .buildUpon()
            .setOverrideForType(
                TrackSelectionOverride(
                    player.currentTracks.groups[groupIndex].mediaTrackGroup,
                    trackIndex
                )
            )
            .build()
        player.trackSelectionParameters = trackSelector
    }

    data class AudioTrackInfo(
        val groupIndex: Int,
        val trackIndex: Int,
        val language: String,
        val label: String,
        val isSelected: Boolean
    )
}
