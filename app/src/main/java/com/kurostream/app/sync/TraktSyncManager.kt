package com.kurostream.app.sync

import com.kurostream.domain.entity.MediaItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TraktSyncManager @Inject constructor() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // ── MediaItem-based API (for callers that already have a full MediaItem) ──

    suspend fun onPlaybackStarted(item: MediaItem, progress: Float) {
        onPlaybackStarted(item.id, null, progress)
    }

    suspend fun onPlaybackPaused(item: MediaItem, progress: Float) {
        onPlaybackPaused(item.id, null, progress)
    }

    suspend fun onPlaybackStopped(item: MediaItem, progress: Float, completed: Boolean = false) {
        onPlaybackStopped(item.id, null, progress)
    }

    suspend fun onProgressUpdate(item: MediaItem, progress: Float) {
        onProgressUpdate(item.id, null, progress)
    }

    // ── String-based API (used by PlayerViewModel) ────────────────────────────

    suspend fun onPlaybackStarted(mediaId: String?, episodeId: String?, progress: Float) {
        scope.launch {
            try {
                Timber.d("TraktSync: scrobble start mediaId=$mediaId ep=$episodeId at ${progress}%")
            } catch (e: Exception) {
                Timber.w(e, "Trakt scrobble start failed")
            }
        }
    }

    suspend fun onPlaybackPaused(mediaId: String?, episodeId: String?, progress: Float) {
        scope.launch {
            try {
                Timber.d("TraktSync: scrobble pause mediaId=$mediaId ep=$episodeId at ${progress}%")
            } catch (e: Exception) {
                Timber.w(e, "Trakt scrobble pause failed")
            }
        }
    }

    suspend fun onPlaybackStopped(mediaId: String?, episodeId: String?, progress: Float) {
        scope.launch {
            try {
                Timber.d("TraktSync: scrobble stop mediaId=$mediaId ep=$episodeId at ${progress}%")
            } catch (e: Exception) {
                Timber.w(e, "Trakt scrobble stop failed")
            }
        }
    }

    suspend fun onProgressUpdate(mediaId: String?, episodeId: String?, progress: Float) {
        scope.launch {
            try {
                Timber.d("TraktSync: scrobble progress mediaId=$mediaId ep=$episodeId at ${progress}%")
            } catch (e: Exception) {
                Timber.w(e, "Trakt scrobble progress failed")
            }
        }
    }

    suspend fun syncHistory() {
        scope.launch {
            try {
                Timber.d("TraktSync: history sync")
            } catch (e: Exception) {
                Timber.w(e, "Trakt history sync failed")
            }
        }
    }

    suspend fun importWatchlist() {
        scope.launch {
            try {
                Timber.d("TraktSync: watchlist import")
            } catch (e: Exception) {
                Timber.w(e, "Trakt watchlist import failed")
            }
        }
    }
}