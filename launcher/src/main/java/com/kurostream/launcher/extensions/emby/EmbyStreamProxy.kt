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

package com.kurostream.launcher.extensions.emby

import com.google.android.exoplayer2.util.MimeTypes
import com.kurostream.launcher.data.model.StreamSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EmbyStreamProxy @Inject constructor(
    private val authManager: EmbyAuthManager
) {

    suspend fun getStreamSource(itemId: String, startPositionMs: Long = 0): Result<StreamSource> =
        withContext(Dispatchers.IO) {
            try {
                val service = authManager.getAuthenticatedService()
                    ?: return@withContext Result.failure(Exception("Not authenticated"))
                val serverUrl = authManager.getServerUrl()?.trimEnd('/')
                    ?: return@withContext Result.failure(Exception("Server URL not found"))

                val directStreamUrl = "$serverUrl/Videos/$itemId/stream?Static=true"
                val transcodeUrl = "$serverUrl/Videos/$itemId/master.m3u8?api_key=${authManager.getAccessToken()}"

                Result.success(
                    StreamSource(
                        id = "emby_$itemId",
                        title = "Emby Stream",
                        url = directStreamUrl,
                        fallbackUrl = transcodeUrl,
                        mimeType = MimeTypes.VIDEO_UNKNOWN,
                        headers = mapOf("X-Emby-Token" to (authManager.getAccessToken() ?: "")),
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
        try {
            service.reportPlaybackProgress(
                EmbyPlaybackProgress(
                    ItemId = itemId,
                    PositionTicks = positionMs * 10000,
                    IsPaused = isPaused
                )
            )
        } catch (e: Exception) { }
    }

    suspend fun reportStarted(itemId: String) {
        val service = authManager.getAuthenticatedService() ?: return
        try {
            service.reportPlaybackStart(EmbyPlaybackProgress(ItemId = itemId, PositionTicks = 0))
        } catch (e: Exception) { }
    }

    suspend fun reportStopped(itemId: String, positionMs: Long) {
        val service = authManager.getAuthenticatedService() ?: return
        try {
            service.reportPlaybackStopped(
                EmbyPlaybackProgress(ItemId = itemId, PositionTicks = positionMs * 10000)
            )
        } catch (e: Exception) { }
    }
}
