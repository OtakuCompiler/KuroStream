package com.kurostream.app.analytics

import android.content.Context
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnalyticsManager @Inject constructor(
    context: Context
) {
    fun logEvent(name: String, params: Map<String, String> = emptyMap()) {
        Timber.d("Analytics: $name $params")
    }

    fun logPlaybackStart(mediaId: String, quality: String, ramUsageMb: Int) {
        logEvent("playback_start", mapOf(
            "media_id" to mediaId,
            "quality" to quality,
            "ram_mb" to ramUsageMb.toString()
        ))
    }

    fun logOom(ramUsageMb: Int, bufferSizeMb: Int) {
        logEvent("oom_crash_avoided", mapOf(
            "ram_mb" to ramUsageMb.toString(),
            "buffer_mb" to bufferSizeMb.toString()
        ))
    }
}
