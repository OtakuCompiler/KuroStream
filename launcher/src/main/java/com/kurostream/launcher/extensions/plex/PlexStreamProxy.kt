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

package com.kurostream.launcher.extensions.plex

import com.google.android.exoplayer2.util.MimeTypes
import com.kurostream.launcher.data.model.StreamSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlexStreamProxy @Inject constructor(
    private val authManager: PlexAuthManager
) {

    suspend fun getStreamSource(ratingKey: String, startPositionMs: Long = 0): Result<StreamSource> =
        withContext(Dispatchers.IO) {
            try {
                val service = authManager.getServerService()
                    ?: return@withContext Result.failure(Exception("Not connected"))
                val serverUrl = authManager.getServerUrl()?.trimEnd('/')
                    ?: return@withContext Result.failure(Exception("Server URL not found"))
                val token = authManager.getAccessToken()
                    ?: return@withContext Result.failure(Exception("Not authenticated"))

                val metadataResponse = service.getMetadata(ratingKey)
                val metadata = metadataResponse.body()?.mediaContainer?.metadata?.firstOrNull()
                val part = metadata?.media?.firstOrNull()?.parts?.firstOrNull()
                val streamPath = part?.key ?: "/library/metadata/$ratingKey"
                val container = part?.container

                val mimeType = when (container) {
                    "mp4" -> MimeTypes.VIDEO_MP4
                    "mkv", "matroska" -> MimeTypes.VIDEO_MATROSKA
                    "webm" -> MimeTypes.VIDEO_WEBM
                    else -> MimeTypes.VIDEO_UNKNOWN
                }

                Result.success(
                    StreamSource(
                        id = "plex_$ratingKey",
                        title = metadata?.title ?: "Plex Stream",
                        url = "$serverUrl$streamPath",
                        fallbackUrl = "$serverUrl/video/:/transcode/universal/start.m3u8?" +
                            "path=$streamPath&mediaIndex=0&partIndex=0&protocol=hls&fastSeek=1&directPlay=0&directStream=1",
                        mimeType = mimeType,
                        headers = mapOf("X-Plex-Token" to token),
                        startPositionMs = startPositionMs,
                        requiresProxy = true,
                        drmScheme = null
                    )
                )
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun reportTimeline(ratingKey: String, state: String, timeMs: Long, durationMs: Long) {
        val service = authManager.getServerService() ?: return
        try {
            service.updateTimeline(
                ratingKey = ratingKey,
                key = "/library/metadata/$ratingKey",
                state = state,
                time = timeMs,
                duration = durationMs
            )
        } catch (e: Exception) { }
    }
}
