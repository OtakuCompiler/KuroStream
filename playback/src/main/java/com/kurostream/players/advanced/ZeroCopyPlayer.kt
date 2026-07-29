package com.kurostream.players.advanced

import android.content.Context
import android.graphics.SurfaceTexture
import android.media.MediaFormat
import android.view.Surface
import com.kurostream.players.backend.MediaCodecSurfaceBackend

class ZeroCopyPlayer(context: Context, private val format: MediaFormat) {
    private val surfaceTexture = SurfaceTexture(false)
    private val surface = Surface(surfaceTexture)
    private val decoder = MediaCodecSurfaceBackend(surface)
    private var isReleased = false

    init {
        try {
            decoder.configure(format)
        } catch (e: Exception) {
            release()
            throw e
        }
    }

    fun release() {
        if (!isReleased) {
            isReleased = true
            decoder.release()
            surface.release()
            surfaceTexture.release()
        }
    }

    fun isReleased(): Boolean = isReleased
}