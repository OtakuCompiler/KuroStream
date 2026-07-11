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

package com.kurostream.app.repository

import com.kurostream.domain.model.SourceLockSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    data class Settings(
        val autoPlayNextEnabled: Boolean = true,
        val skipIntroEnabled: Boolean = true,
        val hardwareAccelerationEnabled: Boolean = true,
        val backgroundPlaybackEnabled: Boolean = false,
        val debugOverlayEnabled: Boolean = false,
        val cacheSizeFormatted: String = "0 MB",
        val preferredAudioLanguages: List<String> = emptyList(),
        val preferredSubtitleLanguages: List<String> = emptyList(),
        val highContrastEnabled: Boolean = false,
        val reduceMotionEnabled: Boolean = false,
        val focusHighlightEnabled: Boolean = true,
        val sourceLockSettings: SourceLockSettings = SourceLockSettings(),
        // Disk Buffer Settings (NEW)
        val diskBufferSizeMb: Int = 200,
        val diskBufferReadAheadMb: Int = 4,
        val diskBufferLocation: String = "internal",
        val diskBufferDeleteOnShutdown: Boolean = false,
        // Torrent Settings (NEW)
        val seedWhileIdleEnabled: Boolean = true,
        val sequentialDownloadEnabled: Boolean = true,
        val seedRatioLimit: Float = 2.0f,
        val globalDownloadLimitKbps: Long = -1L,
        val globalUploadLimitKbps: Long = -1L,
        // AI Features
        val aiUpscalingEnabled: Boolean = false,
        val frameInterpolationEnabled: Boolean = false,
        val lowLatencyUpscalingEnabled: Boolean = false,
        // VOD Cache
        val vodCacheCompressionEnabled: Boolean = true,
    )

    fun getSettings(): Settings
    suspend fun setAutoPlayNextEnabled(enabled: Boolean)
    suspend fun setSkipIntroEnabled(enabled: Boolean)
    suspend fun setHardwareAccelerationEnabled(enabled: Boolean)
    suspend fun setBackgroundPlaybackEnabled(enabled: Boolean)
    suspend fun setDebugOverlayEnabled(enabled: Boolean)
    suspend fun setPreferredAudioLanguages(languages: List<String>)
    suspend fun setPreferredSubtitleLanguages(languages: List<String>)
    suspend fun setHighContrastEnabled(enabled: Boolean)
    suspend fun setReduceMotionEnabled(enabled: Boolean)
    suspend fun setFocusHighlightEnabled(enabled: Boolean)
    suspend fun setSourceLockSettings(settings: SourceLockSettings)
    fun observeSettings(): Flow<Settings>
    suspend fun clearCache()
    fun getCacheSize(): Long
    suspend fun runBenchmarks()

    // Disk Buffer Settings (NEW)
    suspend fun setDiskBufferSizeMb(sizeMb: Int)
    suspend fun setDiskBufferReadAheadMb(sizeMb: Int)
    suspend fun setDiskBufferLocation(location: String)
    suspend fun setDiskBufferDeleteOnShutdown(enabled: Boolean)

    // Torrent Settings (NEW)
    suspend fun setSeedWhileIdleEnabled(enabled: Boolean)
    suspend fun setSequentialDownloadEnabled(enabled: Boolean)
    suspend fun setSeedRatioLimit(limit: Float)
    suspend fun setGlobalDownloadLimit(kbps: Long)
    suspend fun setGlobalUploadLimit(kbps: Long)

    // Source Lock settings
    suspend fun setSourceLockEnabled(enabled: Boolean)
    suspend fun setSourceLockFallbackMode(mode: Int)
    suspend fun setSourceLockMaxRetries(retries: Int)
    suspend fun setSourceLockRetryDelayMs(delay: Long)
    suspend fun setSourceLockPersist(persist: Boolean)
    suspend fun setSourceLockNotifyFallback(notify: Boolean)
    suspend fun clearAllSourceLocks()

    // AI Features
    suspend fun setAiUpscalingEnabled(enabled: Boolean)
    suspend fun setFrameInterpolationEnabled(enabled: Boolean)
    suspend fun setLowLatencyUpscalingEnabled(enabled: Boolean)

    // VOD Cache
    suspend fun setVodCacheCompressionEnabled(enabled: Boolean)
}