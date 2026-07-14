package com.kurostream.common.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.util.Log

class AudioSessionManager(private val context: Context) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null
    private var currentFocus: AudioFocusState = AudioFocusState.NO_FOCUS

    enum class AudioFocusState { NO_FOCUS, TRANSIENT, FULL }

    private val focusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        currentFocus = when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> AudioFocusState.FULL
            AudioManager.AUDIOFOCUS_LOSS -> AudioFocusState.NO_FOCUS
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> AudioFocusState.TRANSIENT
            else -> AudioFocusState.NO_FOCUS
        }
    }

    fun requestAudioFocus(): Boolean {
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
            .setFlags(AudioAttributes.FLAG_LOW_LATENCY)
            .build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(attrs)
                .setAcceptsDelayedFocusGain(true)
                .setOnAudioFocusChangeListener(focusChangeListener)
                .build()
            audioFocusRequest = request
            val result = audioManager.requestAudioFocus(request)
            result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } else {
            @Suppress("DEPRECATION")
            val result = audioManager.requestAudioFocus(
                focusChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN,
            )
            result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
    }

    fun abandonAudioFocus() {
        audioFocusRequest?.let {
            audioManager.abandonAudioFocusRequest(it)
            audioFocusRequest = null
        }
        currentFocus = AudioFocusState.NO_FOCUS
    }
}
