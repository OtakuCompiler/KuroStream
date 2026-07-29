package com.kurostream.players

import androidx.media3.session.MediaSessionService
import androidx.media3.session.MediaSession
import android.content.Intent

class MediaPlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession
    override fun onTaskRemoved(rootIntent: Intent?) {
        mediaSession?.release()
        stopSelf()
    }
    override fun onDestroy() {
        mediaSession?.release()
        super.onDestroy()
    }
}
