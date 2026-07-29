package com.kurostream.data.repository

import com.kurostream.domain.repository.AppTheme
import com.kurostream.domain.repository.PlayerSubtitleSettings
import com.kurostream.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor() : SettingsRepository {
    // ── Existing flows ─────────────────────────────────────────────────
    private val themeFlow = MutableStateFlow(AppTheme.SYSTEM)
    private val dynamicColorsFlow = MutableStateFlow(true)
    private val autoUpdateExtensionsFlow = MutableStateFlow(true)
    private val defaultQualityFlow = MutableStateFlow("auto")
    private val skipIntroFlow = MutableStateFlow(true)
    private val skipOutroFlow = MutableStateFlow(true)
    private val cacheSizeFlow = MutableStateFlow(1024)
    private val skinNameFlow = MutableStateFlow("AMOLED_BLACK")
    private val reduceMotionFlow = MutableStateFlow(false)

    // ── New flows ──────────────────────────────────────────────────────
    private val autoPlayNextFlow = MutableStateFlow(true)
    private val hardwareAccelerationFlow = MutableStateFlow(true)
    private val backgroundPlaybackFlow = MutableStateFlow(false)
    private val highContrastFlow = MutableStateFlow(false)
    private val focusHighlightFlow = MutableStateFlow(true)
    private val sourceLockEnabledFlow = MutableStateFlow(true)
    private val sourceLockFallbackModeFlow = MutableStateFlow(0)
    private val sourceLockMaxRetriesFlow = MutableStateFlow(2)
    private val sourceLockRetryDelayMsFlow = MutableStateFlow(3000L)
    private val sourceLockPersistFlow = MutableStateFlow(true)
    private val sourceLockNotifyFallbackFlow = MutableStateFlow(true)
    private val aiUpscalingFlow = MutableStateFlow(false)
    private val frameInterpolationFlow = MutableStateFlow(false)
    private val lowLatencyUpscalingFlow = MutableStateFlow(false)
    private val diskBufferSizeMbFlow = MutableStateFlow(200)
    private val diskBufferReadAheadMbFlow = MutableStateFlow(4)
    private val diskBufferLocationFlow = MutableStateFlow("internal")
    private val diskBufferDeleteOnShutdownFlow = MutableStateFlow(false)
    private val vodCacheCompressionFlow = MutableStateFlow(true)
    private val seedWhileIdleFlow = MutableStateFlow(true)
    private val sequentialDownloadFlow = MutableStateFlow(true)
    private val seedRatioLimitFlow = MutableStateFlow(2.0f)
    private val globalDownloadLimitFlow = MutableStateFlow(-1L)
    private val globalUploadLimitFlow = MutableStateFlow(-1L)

    // ── Subtitle flows ─────────────────────────────────────────────────
    private val subtitleFontSizeFlow = MutableStateFlow(24f)
    private val subtitleFontColorFlow = MutableStateFlow("#FFFFFF")
    private val subtitleBgColorFlow = MutableStateFlow("#80000000")
    private val subtitleEnabledFlow = MutableStateFlow(true)

    private val playerSubtitleSettingsFlow = MutableStateFlow(PlayerSubtitleSettings())

    // ── Theme ──────────────────────────────────────────────────────────
    override fun observeTheme(): Flow<AppTheme> = themeFlow
    override suspend fun setTheme(theme: AppTheme) { themeFlow.value = theme }
    override fun observeDynamicColorsEnabled(): Flow<Boolean> = dynamicColorsFlow
    override suspend fun setDynamicColorsEnabled(enabled: Boolean) { dynamicColorsFlow.value = enabled }

    // ── Extensions ─────────────────────────────────────────────────────
    override fun observeAutoUpdateExtensions(): Flow<Boolean> = autoUpdateExtensionsFlow
    override suspend fun setAutoUpdateExtensions(enabled: Boolean) { autoUpdateExtensionsFlow.value = enabled }

    // ── Quality ────────────────────────────────────────────────────────
    override fun observeDefaultQuality(): Flow<String> = defaultQualityFlow
    override suspend fun setDefaultQuality(quality: String) { defaultQualityFlow.value = quality }

    // ── Playback ───────────────────────────────────────────────────────
    override fun observeSkipIntroEnabled(): Flow<Boolean> = skipIntroFlow
    override suspend fun setSkipIntroEnabled(enabled: Boolean) { skipIntroFlow.value = enabled }
    override fun observeSkipOutroEnabled(): Flow<Boolean> = skipOutroFlow
    override suspend fun setSkipOutroEnabled(enabled: Boolean) { skipOutroFlow.value = enabled }
    override suspend fun setAutoPlayNextEnabled(enabled: Boolean) { autoPlayNextFlow.value = enabled }
    override suspend fun setHardwareAccelerationEnabled(enabled: Boolean) { hardwareAccelerationFlow.value = enabled }
    override suspend fun setBackgroundPlaybackEnabled(enabled: Boolean) { backgroundPlaybackFlow.value = enabled }

    // ── Cache ──────────────────────────────────────────────────────────
    override fun observeCacheSizeMb(): Flow<Int> = cacheSizeFlow
    override suspend fun setCacheSizeMb(size: Int) { cacheSizeFlow.value = size }

    // ── Skin / Accessibility ──────────────────────────────────────────
    override fun observeSkinName(): Flow<String> = skinNameFlow
    override suspend fun setSkinName(name: String) { skinNameFlow.value = name }
    override fun observeReduceMotionEnabled(): Flow<Boolean> = reduceMotionFlow
    override suspend fun setReduceMotionEnabled(enabled: Boolean) { reduceMotionFlow.value = enabled }
    override suspend fun setHighContrastEnabled(enabled: Boolean) { highContrastFlow.value = enabled }
    override suspend fun setFocusHighlightEnabled(enabled: Boolean) { focusHighlightFlow.value = enabled }

    // ── Source Lock ────────────────────────────────────────────────────
    override suspend fun setSourceLockEnabled(enabled: Boolean) { sourceLockEnabledFlow.value = enabled }
    override suspend fun setSourceLockFallbackMode(mode: Int) { sourceLockFallbackModeFlow.value = mode }
    override suspend fun setSourceLockMaxRetries(retries: Int) { sourceLockMaxRetriesFlow.value = retries }
    override suspend fun setSourceLockRetryDelayMs(delay: Long) { sourceLockRetryDelayMsFlow.value = delay }
    override suspend fun setSourceLockPersist(persist: Boolean) { sourceLockPersistFlow.value = persist }
    override suspend fun setSourceLockNotifyFallback(notify: Boolean) { sourceLockNotifyFallbackFlow.value = notify }
    override suspend fun clearAllSourceLocks() {
        sourceLockEnabledFlow.value = true
        sourceLockFallbackModeFlow.value = 0
        sourceLockMaxRetriesFlow.value = 2
        sourceLockRetryDelayMsFlow.value = 3000L
        sourceLockPersistFlow.value = true
        sourceLockNotifyFallbackFlow.value = true
    }

    // ── AI / Upscaling ────────────────────────────────────────────────
    override suspend fun setAiUpscalingEnabled(enabled: Boolean) { aiUpscalingFlow.value = enabled }
    override suspend fun setFrameInterpolationEnabled(enabled: Boolean) { frameInterpolationFlow.value = enabled }
    override suspend fun setLowLatencyUpscalingEnabled(enabled: Boolean) { lowLatencyUpscalingFlow.value = enabled }

    // ── Disk Buffer ────────────────────────────────────────────────────
    override suspend fun setDiskBufferSizeMb(sizeMb: Int) { diskBufferSizeMbFlow.value = sizeMb }
    override suspend fun setDiskBufferReadAheadMb(sizeMb: Int) { diskBufferReadAheadMbFlow.value = sizeMb }
    override suspend fun setDiskBufferLocation(location: String) { diskBufferLocationFlow.value = location }
    override suspend fun setDiskBufferDeleteOnShutdown(enabled: Boolean) { diskBufferDeleteOnShutdownFlow.value = enabled }
    override suspend fun setVodCacheCompressionEnabled(enabled: Boolean) { vodCacheCompressionFlow.value = enabled }

    // ── Torrent ────────────────────────────────────────────────────────
    override suspend fun setSeedWhileIdleEnabled(enabled: Boolean) { seedWhileIdleFlow.value = enabled }
    override suspend fun setSequentialDownloadEnabled(enabled: Boolean) { sequentialDownloadFlow.value = enabled }
    override suspend fun setSeedRatioLimit(limit: Float) { seedRatioLimitFlow.value = limit }
    override suspend fun setGlobalDownloadLimit(kbps: Long) { globalDownloadLimitFlow.value = kbps }
    override suspend fun setGlobalUploadLimit(kbps: Long) { globalUploadLimitFlow.value = kbps }

    // ── Player Subtitle ───────────────────────────────────────────────
    override fun observePlayerSubtitleSettings(): Flow<PlayerSubtitleSettings> = playerSubtitleSettingsFlow
    override fun getPlayerSubtitleSettings(): PlayerSubtitleSettings = playerSubtitleSettingsFlow.value
    override suspend fun setSubtitleFontSize(size: Float) {
        subtitleFontSizeFlow.value = size
        playerSubtitleSettingsFlow.value = playerSubtitleSettingsFlow.value.copy(fontSize = size)
    }
    override suspend fun setSubtitleFontColor(hex: String) {
        subtitleFontColorFlow.value = hex
        playerSubtitleSettingsFlow.value = playerSubtitleSettingsFlow.value.copy(fontColorHex = hex)
    }
    override suspend fun setSubtitleBgColor(hex: String) {
        subtitleBgColorFlow.value = hex
        playerSubtitleSettingsFlow.value = playerSubtitleSettingsFlow.value.copy(bgColorHex = hex)
    }
    override suspend fun setSubtitleEnabled(enabled: Boolean) {
        subtitleEnabledFlow.value = enabled
        playerSubtitleSettingsFlow.value = playerSubtitleSettingsFlow.value.copy(enabled = enabled)
    }

    // ── Bulk ───────────────────────────────────────────────────────────
    override suspend fun clearAllSettings() {
        themeFlow.value = AppTheme.SYSTEM
        dynamicColorsFlow.value = true
        autoUpdateExtensionsFlow.value = true
        defaultQualityFlow.value = "auto"
        skipIntroFlow.value = true
        skipOutroFlow.value = true
        cacheSizeFlow.value = 1024
        skinNameFlow.value = "AMOLED_BLACK"
        reduceMotionFlow.value = false
    }

    // ── Generic key-value ──────────────────────────────────────────────
    private val genericStore = mutableMapOf<String, Any?>()

    @Suppress("UNCHECKED_CAST")
    override suspend fun <T> getSetting(key: String): T? = genericStore[key] as? T

    override suspend fun <T> setSetting(key: String, value: T) {
        genericStore[key] = value
    }
}
