package com.kurostream.players.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioPassthroughManager @Inject constructor(
    private val context: Context
) {
    private val _isPassthroughEnabled = MutableStateFlow(false)
    val isPassthroughEnabled: StateFlow<Boolean> = _isPassthroughEnabled.asStateFlow()

    private val _forcedBitstream = MutableStateFlow(false)
    val forcedBitstream: StateFlow<Boolean> = _forcedBitstream.asStateFlow()

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    fun enablePassthrough(enabled: Boolean) {
        _isPassthroughEnabled.value = enabled
        applyAudioConfiguration()
    }

    fun forceHDMIBitstream(enabled: Boolean) {
        _forcedBitstream.value = enabled
        if (enabled) enablePassthrough(true)
    }

    private fun applyAudioConfiguration() {
        if (_isPassthroughEnabled.value || _forcedBitstream.value) {
            configurePassthroughMode()
        } else {
            configureStereoMode()
        }
    }

    private fun configurePassthroughMode() {
        val audioFormat = AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_AC3)
            .setSampleRate(48000)
            .setChannelMask(AudioFormat.CHANNEL_OUT_5_1)
            .build()

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
            .setFlags(AudioAttributes.FLAG_LOW_LATENCY)
            .build()

        val minBufferSize = AudioTrack.getMinBufferSize(
            audioFormat.sampleRate,
            audioFormat.channelMask,
            audioFormat.encoding
        ).takeIf { it > 0 } ?: return

        AudioTrack(
            audioAttributes,
            audioFormat,
            minBufferSize,
            AudioTrack.MODE_STREAM,
            AudioManager.AUDIO_SESSION_ID_GENERATE
        )?.setPreferredDevice(
            audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
                .find { device ->
                    device.isSink &&
                    (device.type == android.media.AudioDeviceInfo.TYPE_HDMI ||
                     device.type == android.media.AudioDeviceInfo.TYPE_HDMI_ARC)
                }
        )
    }

    private fun configureStereoMode() {
        audioManager.mode = AudioManager.MODE_NORMAL
        audioManager.setStreamMute(AudioManager.STREAM_MUSIC, false)
    }

    fun isAtmosSupported(): Boolean =
        audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            .any { it.isSink && (it.encodings.contains(AudioFormat.ENCODING_AC4) || it.encodings.contains(AudioFormat.ENCODING_E_AC3_JOC)) }

    fun isDTSXSupported(): Boolean =
        audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            .any { it.isSink && (it.encodings.contains(AudioFormat.ENCODING_DTS) || it.encodings.contains(AudioFormat.ENCODING_DTS_HD)) }

    fun getSupportedFormats(): List<String> {
        val formatMap = mapOf(
            AudioFormat.ENCODING_AC3 to "Dolby Digital",
            AudioFormat.ENCODING_E_AC3 to "Dolby Digital Plus",
            AudioFormat.ENCODING_AC4 to "Dolby Atmos (AC-4)",
            AudioFormat.ENCODING_E_AC3_JOC to "Dolby Atmos (E-AC-3 JOC)",
            AudioFormat.ENCODING_DTS to "DTS",
            AudioFormat.ENCODING_DTS_HD to "DTS-HD",
        )

        return audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            .flatMap { device ->
                if (device.isSink) device.encodings.mapNotNull { formatMap[it] } else emptyList()
            }
            .distinct()
    }
}
