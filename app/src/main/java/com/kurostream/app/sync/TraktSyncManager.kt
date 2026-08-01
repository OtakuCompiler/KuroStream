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

    suspend fun onPlaybackStarted(item: MediaItem, progress: Float) {
        scope.launch {
            try {
                Timber.d("TraktSync: scrobble start for ${item.title} at ${progress}%")
            } catch (e: Exception) {
                Timber.w(e, "Trakt scrobble start failed")
            }
        }
    }

    suspend fun onPlaybackPaused(item: MediaItem, progress: Float) {
        scope.launch {
            try {
                Timber.d("TraktSync: scrobble pause for ${item.title} at ${progress}%")
            } catch (e: Exception) {
                Timber.w(e, "Trakt scrobble pause failed")
            }
        }
    }

    suspend fun onPlaybackStopped(item: MediaItem, progress: Float, completed: Boolean) {
        scope.launch {
            try {
                Timber.d("TraktSync: scrobble stop for ${item.title} at ${progress}%, completed=$completed")
            } catch (e: Exception) {
                Timber.w(e, "Trakt scrobble stop failed")
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