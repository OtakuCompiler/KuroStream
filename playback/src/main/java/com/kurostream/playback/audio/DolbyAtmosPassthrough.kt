package com.kurostream.playback.audio

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import timber.log.Timber

class DolbyAtmosPassthrough(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    fun isPassthroughSupported(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            devices.any { it.type == AudioDeviceInfo.TYPE_HDMI || it.type == AudioDeviceInfo.TYPE_HDMI_ARC }
        } else {
            context.packageManager.hasSystemFeature("android.software.leanback")
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    fun isDolbyAtmosSupported(): Boolean {
        if (!isPassthroughSupported()) return false
        val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        return devices.any { device ->
            device.encodings.contains(android.media.AudioFormat.ENCODING_E_AC3_JOC)
        }
    }

    fun createPassthroughAudioSink(): AudioSink {
        return DefaultAudioSink.Builder(context)
            .setEnableAudioTrackPlaybackParams(true)
            .build()
    }

    fun getAudioEncoding(format: Format): Int {
        return when (format.sampleMimeType) {
            MimeTypes.AUDIO_AC3 -> C.ENCODING_AC3
            MimeTypes.AUDIO_E_AC3 -> C.ENCODING_E_AC3
            MimeTypes.AUDIO_E_AC3_JOC -> C.ENCODING_E_AC3_JOC
            MimeTypes.AUDIO_TRUEHD -> C.ENCODING_DOLBY_TRUEHD
            MimeTypes.AUDIO_DTS -> C.ENCODING_DTS
            MimeTypes.AUDIO_DTS_HD -> C.ENCODING_DTS_HD
            MimeTypes.AUDIO_DTS_EXPRESS -> C.ENCODING_DTS_HD
            else -> C.ENCODING_PCM_16BIT
        }
    }

    fun logAudioCapabilities() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            devices.forEach { device ->
                val encodings = device.encodings.joinToString { encodingToString(it) }
                Timber.d("Audio device: ${device.productName}, type=${device.type}, encodings=[$encodings]")
            }
        }
    }

    private fun encodingToString(encoding: Int): String {
        return when (encoding) {
            android.media.AudioFormat.ENCODING_PCM_16BIT -> "PCM_16BIT"
            android.media.AudioFormat.ENCODING_AC3 -> "AC3"
            android.media.AudioFormat.ENCODING_E_AC3 -> "E-AC3"
            android.media.AudioFormat.ENCODING_E_AC3_JOC -> "E-AC3-JOC (Atmos)"
            android.media.AudioFormat.ENCODING_DOLBY_TRUEHD -> "TRUEHD"
            android.media.AudioFormat.ENCODING_DTS -> "DTS"
            android.media.AudioFormat.ENCODING_DTS_HD -> "DTS-HD"
            else -> "UNKNOWN($encoding)"
        }
    }
}
