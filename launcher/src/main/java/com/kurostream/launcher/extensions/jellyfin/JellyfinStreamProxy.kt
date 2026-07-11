// This file is part of KuroStream.
//
// KuroStream is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// KuroStream is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with KuroStream.  If not, see <https://www.gnu.org/licenses/>.

package com.kurostream.launcher.extensions.jellyfin

import android.net.Uri
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.util.MimeTypes
import com.kurostream.launcher.data.model.StreamSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JellyfinStreamProxy @Inject constructor(
    private val authManager: JellyfinAuthManager
) {

    suspend fun getStreamSource(itemId: String, startPositionMs: Long = 0): Result<StreamSource> =
        withContext(Dispatchers.IO) {
            try {
                val service = authManager.getAuthenticatedService()
                    ?: return@withContext Result.failure(Exception("Not authenticated"))

                val userId = authManager.getUserId()
                    ?: return@withContext Result.failure(Exception("User ID not found"))

                val serverUrl = authManager.getServerUrl()?.trimEnd('/')
                    ?: return@withContext Result.failure(Exception("Server URL not found"))

                // Get playback info to determine if transcoding is needed
                val playbackInfo = service.getPlaybackInfo(itemId, userId)
                val playSessionId = playbackInfo.body()?.PlaySessionId

                // Build direct stream URL
                val directStreamUrl = "$serverUrl/Videos/$itemId/stream?Static=true"

                // Build transcoding URL as fallback
                val transcodeUrl = "$serverUrl/Videos/$itemId/master.m3u8?" +
                    "MediaSourceId=$itemId&" +
                    "PlaySessionId=$playSessionId&" +
                    "api_key=${authManager.getAccessToken()}"

                // Try direct play first
                val mediaSource = playbackInfo.body()?.MediaSources?.firstOrNull()
                val container = mediaSource?.Container
                val mimeType = when (container) {
                    "mkv", "matroska" -> MimeTypes.VIDEO_MATROSKA
                    "mp4" -> MimeTypes.VIDEO_MP4
                    "webm" -> MimeTypes.VIDEO_WEBM
                    else -> MimeTypes.VIDEO_UNKNOWN
                }

                Result.success(
                    StreamSource(
                        id = "jellyfin_$itemId",
                        title = "Jellyfin Stream",
                        url = directStreamUrl,
                        fallbackUrl = transcodeUrl,
                        mimeType = mimeType,
                        headers = mapOf(
                            "X-Emby-Token" to (authManager.getAccessToken() ?: ""),
                            "X-Emby-Authorization" to "MediaBrowser Client="StreamBox""
                        ),
                        startPositionMs = startPositionMs,
                        requiresProxy = true,
                        drmScheme = null
                    )
                )
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun reportProgress(itemId: String, positionMs: Long, isPaused: Boolean) {
        val service = authManager.getAuthenticatedService() ?: return
        val userId = authManager.getUserId() ?: return

        val progress = JellyfinPlaybackProgress(
            ItemId = itemId,
            PositionTicks = positionMs * 10000, // Convert to ticks
            IsPaused = isPaused
        )

        try {
            service.reportPlaybackProgress(progress)
        } catch (e: Exception) {
            // Silently fail - progress reporting is non-critical
        }
    }

    suspend fun reportStarted(itemId: String) {
        val service = authManager.getAuthenticatedService() ?: return
        val progress = JellyfinPlaybackProgress(
            ItemId = itemId,
            PositionTicks = 0
        )
        try {
            service.reportPlaybackStart(progress)
        } catch (e: Exception) { }
    }

    suspend fun reportStopped(itemId: String, positionMs: Long) {
        val service = authManager.getAuthenticatedService() ?: return
        val progress = JellyfinPlaybackProgress(
            ItemId = itemId,
            PositionTicks = positionMs * 10000
        )
        try {
            service.reportPlaybackStopped(progress)
        } catch (e: Exception) { }
    }
}
