package com.kurostream.players.selector

import android.content.Context
import com.kurostream.players.media3.Media3Player
import com.kurostream.players.mpv.MpvPlayer
import com.kurostream.players.vlc.VlcPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Backend selection logic.
 *
 * Selection order / policy:
 * 1. If [PlayerBackend] is explicitly requested, try that backend first.
 * 2. If [PlayerBackend.AUTO] (default), probe device capabilities and pick the
 *    most capable backend that initializes successfully.
 * 3. Fallback chain: MEDIA3 → VLC → MPV.
 *
 * Media3 (ExoPlayer) is the most reliable on Android TV/Fire TV and supports
 * hardware decoders, adaptive streaming, and DRM out of the box. VLC adds
 * broader codec coverage for formats Media3 may miss. MPV adds additional
 * format support and is preferred when available, but is the least tested
 * backend in this codebase.
 */
@Singleton
class BackendSelector @Inject constructor(
    private val context: Context,
) {

    suspend fun selectBackend(requested: PlayerBackend = PlayerBackend.AUTO): PlaybackEngine {
        return when (requested) {
            PlayerBackend.MEDIA3 -> createMedia3()
            PlayerBackend.VLC -> createVlc()
            PlayerBackend.MPV -> createMpv()
            PlayerBackend.TORRENT -> throw IllegalStateException("Torrent backend not yet implemented")
            PlayerBackend.AUTO -> autoSelect()
        }
    }

    private suspend fun autoSelect(): PlaybackEngine {
        try {
            return createMedia3()
        } catch (e: Exception) {
            Log.w(TAG, "Media3 init failed", e)
        }

        try {
            return createMpv()
        } catch (e: Exception) {
            Log.w(TAG, "MPV init failed", e)
        }

        return createVlc()
    }

    private suspend fun createMedia3(): PlaybackEngine {
        return withContext(Dispatchers.Main) {
            Media3Player(context).also { it.initialize() }
        }
    }

    private suspend fun createVlc(): PlaybackEngine {
        return withContext(Dispatchers.Main) {
            VlcPlayer(context).also { it.initialize() }
        }
    }

    private suspend fun createMpv(): PlaybackEngine {
        return withContext(Dispatchers.Main) {
            val player = MpvPlayer(context)
            player.initialize()
            if (player.isLibLoaded()) {
                player
            } else {
                throw IllegalStateException("MPV native library not available")
            }
        }
    }

    companion object {
        private const val TAG = "BackendSelector"
        private const val CODEC_MIME_AVC = "video/avc"
        private const val CODEC_MIME_HEVC = "video/hevc"
        private const val CODEC_MIME_AV1 = "video/av1"
    }
}
