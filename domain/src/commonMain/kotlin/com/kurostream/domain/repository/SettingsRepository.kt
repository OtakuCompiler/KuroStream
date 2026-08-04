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

package com.kurostream.domain.repository

import kotlinx.coroutines.flow.Flow

/** Snapshot of player subtitle settings used by PlayerViewModel. */
data class PlayerSubtitleSettings(
    val fontSize: Float = 24f,
    val fontColorHex: String = "#FFFFFF",
    val bgColorHex: String = "#80000000",
    val enabled: Boolean = true,
)

interface SettingsRepository {
    // ── Theme ──────────────────────────────────────────────────────────
    fun observeTheme(): Flow<AppTheme>
    suspend fun setTheme(theme: AppTheme)
    fun observeDynamicColorsEnabled(): Flow<Boolean>
    suspend fun setDynamicColorsEnabled(enabled: Boolean)

    // ── Extensions ─────────────────────────────────────────────────────
    fun observeAutoUpdateExtensions(): Flow<Boolean>
    suspend fun setAutoUpdateExtensions(enabled: Boolean)

    // ── Quality ────────────────────────────────────────────────────────
    fun observeDefaultQuality(): Flow<String>
    suspend fun setDefaultQuality(quality: String)

    // ── Playback ───────────────────────────────────────────────────────
    fun observeSkipIntroEnabled(): Flow<Boolean>
    suspend fun setSkipIntroEnabled(enabled: Boolean)
    fun observeSkipOutroEnabled(): Flow<Boolean>
    suspend fun setSkipOutroEnabled(enabled: Boolean)
    suspend fun setAutoPlayNextEnabled(enabled: Boolean)
    suspend fun setHardwareAccelerationEnabled(enabled: Boolean)
    suspend fun setBackgroundPlaybackEnabled(enabled: Boolean)

    // ── Cache ──────────────────────────────────────────────────────────
    fun observeCacheSizeMb(): Flow<Int>
    suspend fun setCacheSizeMb(size: Int)

    // ── Skin / Accessibility ──────────────────────────────────────────
    fun observeSkinName(): Flow<String>
    suspend fun setSkinName(name: String)
    fun observeReduceMotionEnabled(): Flow<Boolean>
    suspend fun setReduceMotionEnabled(enabled: Boolean)
    suspend fun setHighContrastEnabled(enabled: Boolean)
    suspend fun setFocusHighlightEnabled(enabled: Boolean)

    // ── Source Lock ────────────────────────────────────────────────────
    suspend fun setSourceLockEnabled(enabled: Boolean)
    suspend fun setSourceLockFallbackMode(mode: Int)
    suspend fun setSourceLockMaxRetries(retries: Int)
    suspend fun setSourceLockRetryDelayMs(delay: Long)
    suspend fun setSourceLockPersist(persist: Boolean)
    suspend fun setSourceLockNotifyFallback(notify: Boolean)
    suspend fun clearAllSourceLocks()

    // ── AI / Upscaling ────────────────────────────────────────────────
    suspend fun setAiUpscalingEnabled(enabled: Boolean)
    suspend fun setFrameInterpolationEnabled(enabled: Boolean)
    suspend fun setLowLatencyUpscalingEnabled(enabled: Boolean)

    // ── Disk Buffer ────────────────────────────────────────────────────
    suspend fun setDiskBufferSizeMb(sizeMb: Int)
    suspend fun setDiskBufferReadAheadMb(sizeMb: Int)
    suspend fun setDiskBufferLocation(location: String)
    suspend fun setDiskBufferDeleteOnShutdown(enabled: Boolean)
    suspend fun setVodCacheCompressionEnabled(enabled: Boolean)

    // ── Player Subtitle ───────────────────────────────────────────────
    fun observePlayerSubtitleSettings(): Flow<PlayerSubtitleSettings>
    fun getPlayerSubtitleSettings(): PlayerSubtitleSettings
    suspend fun setSubtitleFontSize(size: Float)
    suspend fun setSubtitleFontColor(hex: String)
    suspend fun setSubtitleBgColor(hex: String)
    suspend fun setSubtitleEnabled(enabled: Boolean)

    // ── Audio ──────────────────────────────────────────────────────────
    suspend fun setAudioPassthroughEnabled(enabled: Boolean)

    // ── Bulk ───────────────────────────────────────────────────────────
    suspend fun clearAllSettings()

    // ── Generic key-value ──────────────────────────────────────────────
    suspend fun <T> getSetting(key: String): T?
    suspend fun <T> setSetting(key: String, value: T)
}

enum class AppTheme { SYSTEM, LIGHT, DARK, OLED }
