package com.kurostream.players.skip

import android.content.Context
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SkipDetectionEngine(context: Context) {

    private val prefs = context.getSharedPreferences("skip_markers", Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _skipIntro = MutableStateFlow(false)
    private val _skipOutro = MutableStateFlow(false)
    val skipIntro: StateFlow<Boolean> = _skipIntro
    val skipOutro: StateFlow<Boolean> = _skipOutro

    fun loadMarkers(mediaId: String): SkipMarkers? {
        val introStart = prefs.getLong("${mediaId}_intro_start", -1)
        val introEnd = prefs.getLong("${mediaId}_intro_end", -1)
        val outroStart = prefs.getLong("${mediaId}_outro_start", -1)
        val outroEnd = prefs.getLong("${mediaId}_outro_end", -1)
        return if (introStart >= 0 && introEnd >= 0) {
            SkipMarkers(introStart, introEnd, outroStart, outroEnd)
        } else null
    }

    fun saveMarkers(mediaId: String, markers: SkipMarkers) {
        prefs.edit()
            .putLong("${mediaId}_intro_start", markers.introStartMs)
            .putLong("${mediaId}_intro_end", markers.introEndMs)
            .putLong("${mediaId}_outro_start", markers.outroStartMs)
            .putLong("${mediaId}_outro_end", markers.outroEndMs)
            .apply()
    }

    fun checkPosition(mediaId: String, positionMs: Long) {
        val markers = loadMarkers(mediaId) ?: return
        _skipIntro.value = positionMs in markers.introStartMs..markers.introEndMs
        _skipOutro.value = positionMs in markers.outroStartMs..markers.outroEndMs
    }

    data class SkipMarkers(
        val introStartMs: Long,
        val introEndMs: Long,
        val outroStartMs: Long,
        val outroEndMs: Long
    )
}
