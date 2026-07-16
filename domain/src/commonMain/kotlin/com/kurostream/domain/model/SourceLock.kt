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

package com.kurostream.domain.model
import com.kurostream.core.platform.platformCurrentTimeMillis
import com.kurostream.domain.entity.MediaItem

import kotlinx.serialization.Serializable

/**
 * Source Lock — Binge-watching consistency feature.
 * When enabled, remembers the source provider for a series and reuses it for subsequent episodes.
 */
@Serializable
data class SourceLock(
    val seriesId: String,
    val providerId: String,           // e.g., "torrserver", "stremio:community", "jellyfin:local"
    val sourceQuality: String,        // e.g., "1080p", "4K HDR", "720p"
    val sourceUrl: String,            // Base URL or identifier for the source
    val lockedAt: Long = platformCurrentTimeMillis(),
    val lastUsedAt: Long = platformCurrentTimeMillis(),
    val episodeCount: Int = 1,        // How many episodes played from this lock
    val isActive: Boolean = true,
)

/** Fallback behavior when locked source fails */
enum class SourceLockFallback {
    AUTOMATIC,  // Silently fall back to best available source
    MANUAL,     // Pause playback, show source picker dialog
}

/** Source lock settings */
@Serializable
data class SourceLockSettings(
    val enabled: Boolean = true,
    val fallbackMode: SourceLockFallback = SourceLockFallback.AUTOMATIC,
    val maxRetries: Int = 2,              // Retries before fallback
    val retryDelayMs: Long = 3000,        // Delay between retries
    val persistAcrossSessions: Boolean = true,
    val notifyOnFallback: Boolean = true, // Show toast/notification when fallback occurs
)

/** Result of source lock resolution */
sealed interface SourceLockResult {
    data class Locked(val mediaItem: MediaItem, val lock: SourceLock) : SourceLockResult
    data class Fallback(val mediaItem: MediaItem, val reason: String) : SourceLockResult
    data class ManualPickerRequired(val seriesId: String, val availableSources: List<MediaSource>) : SourceLockResult
    data class Error(val message: String) : SourceLockResult
}

/** Media source for picker dialog */
@Serializable
data class MediaSource(
    val providerId: String,
    val providerName: String,
    val quality: String,
    val url: String,
    val isLocked: Boolean = false,
    val requiresAuth: Boolean = false,
)