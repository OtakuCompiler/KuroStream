package com.kurostream.app.repository

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferencesKeys
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.runBlocking
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : SettingsRepository {

    private val dataStore = context.preferencesDataStore("kurostream_settings")

    private object Keys {
        val AUTO_PLAY_NEXT = booleanPreferencesKey("auto_play_next", true)
        val SKIP_INTRO = booleanPreferencesKey("skip_intro", true)
        val HARDWARE_ACCELERATION = booleanPreferencesKey("hardware_acceleration", true)
        val BACKGROUND_PLAYBACK = booleanPreferencesKey("background_playback", false)
        val DEBUG_OVERLAY = booleanPreferencesKey("debug_overlay", false)
        val CACHE_SIZE = longPreferencesKey("cache_size", 0L)
        val AUDIO_LANGUAGES = stringPreferencesKey("audio_languages", "")
        val SUBTITLE_LANGUAGES = stringPreferencesKey("subtitle_languages", "")
        val HIGH_CONTRAST = booleanPreferencesKey("high_contrast", false)
        val REDUCE_MOTION = booleanPreferencesKey("reduce_motion", false)
        val FOCUS_HIGHLIGHT = booleanPreferencesKey("focus_highlight", true)

        val SOURCE_LOCK_ENABLED = booleanPreferencesKey("source_lock_enabled", true)
        val SOURCE_LOCK_FALLBACK_MODE = intPreferencesKey("source_lock_fallback_mode", 0)
        val SOURCE_LOCK_MAX_RETRIES = intPreferencesKey("source_lock_max_retries", 2)
        val SOURCE_LOCK_RETRY_DELAY_MS = longPreferencesKey("source_lock_retry_delay_ms", 3000)
        val SOURCE_LOCK_PERSIST = booleanPreferencesKey("source_lock_persist", true)
        val SOURCE_LOCK_NOTIFY_FALLBACK = booleanPreferencesKey("source_lock_notify_fallback", true)

        val DISK_BUFFER_SIZE_MB = intPreferencesKey("disk_buffer_size_mb", 200)
        val DISK_BUFFER_READ_AHEAD_MB = intPreferencesKey("disk_buffer_read_ahead_mb", 4)
        val DISK_BUFFER_LOCATION = stringPreferencesKey("disk_buffer_location", "internal")
        val DISK_BUFFER_DELETE_ON_SHUTDOWN = booleanPreferencesKey("disk_buffer_delete_on_shutdown", false)

        val SEED_WHILE_IDLE = booleanPreferencesKey("seed_while_idle", true)
        val SEQUENTIAL_DOWNLOAD = booleanPreferencesKey("sequential_download", true)
        val SEED_RATIO_LIMIT = floatPreferencesKey("seed_ratio_limit", 2.0f)
        val GLOBAL_DOWNLOAD_LIMIT_KBPS = longPreferencesKey("global_download_limit_kbps", -1L)
        val GLOBAL_UPLOAD_LIMIT_KBPS = longPreferencesKey("global_upload_limit_kbps", -1L)

        val AI_UPSCALING = booleanPreferencesKey("ai_upscaling", false)
        val FRAME_INTERPOLATION = booleanPreferencesKey("frame_interpolation", false)
        val LOW_LATENCY_UPSCALING = booleanPreferencesKey("low_latency_upscaling", false)

        val VOD_CACHE_COMPRESSION = booleanPreferencesKey("vod_cache_compression", true)

        val LIVE_WALLPAPER_ENABLED = booleanPreferencesKey("live_wallpaper_enabled", false)
        val LIVE_WALLPAPER_TYPE = stringPreferencesKey("live_wallpaper_type", "CHERRY_BLOSSOM")
    }

    override fun getSettings(): Settings {
        return dataStore.data.map { prefs ->
            buildSettings(prefs)
        }.blockingFirst()
    }

    override fun observeSettings(): Flow<Settings> {
        return dataStore.data.map { prefs ->
            buildSettings(prefs)
        }
    }

    private fun buildSettings(prefs: Preferences): Settings {
        return Settings(
            autoPlayNextEnabled = getBool(prefs, Keys.AUTO_PLAY_NEXT, true),
            skipIntroEnabled = getBool(prefs, Keys.SKIP_INTRO, true),
            hardwareAccelerationEnabled = getBool(prefs, Keys.HARDWARE_ACCELERATION, true),
            backgroundPlaybackEnabled = getBool(prefs, Keys.BACKGROUND_PLAYBACK, false),
            debugOverlayEnabled = getBool(prefs, Keys.DEBUG_OVERLAY, false),
            cacheSizeFormatted = formatCacheSize(prefs[Keys.CACHE_SIZE] ?: 0L),
            preferredAudioLanguages = parseLanguages(prefs[Keys.AUDIO_LANGUAGES] ?: ""),
            preferredSubtitleLanguages = parseLanguages(prefs[Keys.SUBTITLE_LANGUAGES] ?: ""),
            highContrastEnabled = getBool(prefs, Keys.HIGH_CONTRAST, false),
            reduceMotionEnabled = getBool(prefs, Keys.REDUCE_MOTION, false),
            focusHighlightEnabled = getBool(prefs, Keys.FOCUS_HIGHLIGHT, true),
            sourceLockSettings = buildSourceLockSettings(prefs),
            diskBufferSizeMb = getInt(prefs, Keys.DISK_BUFFER_SIZE_MB, 200),
            diskBufferReadAheadMb = getInt(prefs, Keys.DISK_BUFFER_READ_AHEAD_MB, 4),
            diskBufferLocation = getString(prefs, Keys.DISK_BUFFER_LOCATION, "internal"),
            diskBufferDeleteOnShutdown = getBool(prefs, Keys.DISK_BUFFER_DELETE_ON_SHUTDOWN, false),
            seedWhileIdleEnabled = getBool(prefs, Keys.SEED_WHILE_IDLE, true),
            sequentialDownloadEnabled = getBool(prefs, Keys.SEQUENTIAL_DOWNLOAD, true),
            seedRatioLimit = getFloat(prefs, Keys.SEED_RATIO_LIMIT, 2.0f),
            globalDownloadLimitKbps = getLong(prefs, Keys.GLOBAL_DOWNLOAD_LIMIT_KBPS, -1L),
            globalUploadLimitKbps = getLong(prefs, Keys.GLOBAL_UPLOAD_LIMIT_KBPS, -1L),
            aiUpscalingEnabled = getBool(prefs, Keys.AI_UPSCALING, false),
            frameInterpolationEnabled = getBool(prefs, Keys.FRAME_INTERPOLATION, false),
            lowLatencyUpscalingEnabled = getBool(prefs, Keys.LOW_LATENCY_UPSCALING, false),
            vodCacheCompressionEnabled = getBool(prefs, Keys.VOD_CACHE_COMPRESSION, true),
            liveWallpaperEnabled = getBool(prefs, Keys.LIVE_WALLPAPER_ENABLED, false),
            liveWallpaperType = getString(prefs, Keys.LIVE_WALLPAPER_TYPE, "CHERRY_BLOSSOM"),
        )
    }

    private fun buildSourceLockSettings(prefs: Preferences) = com.kurostream.domain.model.SourceLockSettings(
        enabled = getBool(prefs, Keys.SOURCE_LOCK_ENABLED, true),
        fallbackMode = com.kurostream.domain.model.SourceLockFallback.values()[getInt(prefs, Keys.SOURCE_LOCK_FALLBACK_MODE, 0)],
        maxRetries = getInt(prefs, Keys.SOURCE_LOCK_MAX_RETRIES, 2),
        retryDelayMs = getLong(prefs, Keys.SOURCE_LOCK_RETRY_DELAY_MS, 3000),
        persistAcrossSessions = getBool(prefs, Keys.SOURCE_LOCK_PERSIST, true),
        notifyOnFallback = getBool(prefs, Keys.SOURCE_LOCK_NOTIFY_FALLBACK, true),
    )

    private inline fun getBool(prefs: Preferences, key: Preferences.Key<Boolean>, default: Boolean) = prefs[key] ?: default
    private inline fun getInt(prefs: Preferences, key: Preferences.Key<Int>, default: Int) = prefs[key] ?: default
    private inline fun getLong(prefs: Preferences, key: Preferences.Key<Long>, default: Long) = prefs[key] ?: default
    private inline fun getFloat(prefs: Preferences, key: Preferences.Key<Float>, default: Float) = prefs[key] ?: default
    private inline fun getString(prefs: Preferences, key: Preferences.Key<String>, default: String) = prefs[key] ?: default

    override suspend fun setAutoPlayNextEnabled(enabled: Boolean) {
        dataStore.updateDataAsync { it[Keys.AUTO_PLAY_NEXT] = enabled }
    }

    override suspend fun setSkipIntroEnabled(enabled: Boolean) {
        dataStore.updateDataAsync { it[Keys.SKIP_INTRO] = enabled }
    }

    override suspend fun setHardwareAccelerationEnabled(enabled: Boolean) {
        dataStore.updateDataAsync { it[Keys.HARDWARE_ACCELERATION] = enabled }
    }

    override suspend fun setBackgroundPlaybackEnabled(enabled: Boolean) {
        dataStore.updateDataAsync { it[Keys.BACKGROUND_PLAYBACK] = enabled }
    }

    override suspend fun setDebugOverlayEnabled(enabled: Boolean) {
        dataStore.updateDataAsync { it[Keys.DEBUG_OVERLAY] = enabled }
    }

    override suspend fun setPreferredAudioLanguages(languages: List<String>) {
        dataStore.updateDataAsync { it[Keys.AUDIO_LANGUAGES] = languages.joinToString(",") }
    }

    override suspend fun setPreferredSubtitleLanguages(languages: List<String>) {
        dataStore.updateDataAsync { it[Keys.SUBTITLE_LANGUAGES] = languages.joinToString(",") }
    }

    override suspend fun setHighContrastEnabled(enabled: Boolean) {
        dataStore.updateDataAsync { it[Keys.HIGH_CONTRAST] = enabled }
    }

    override suspend fun setReduceMotionEnabled(enabled: Boolean) {
        dataStore.updateDataAsync { it[Keys.REDUCE_MOTION] = enabled }
    }

    override suspend fun setFocusHighlightEnabled(enabled: Boolean) {
        dataStore.updateDataAsync { it[Keys.FOCUS_HIGHLIGHT] = enabled }
    }

    override suspend fun setSourceLockSettings(settings: com.kurostream.domain.model.SourceLockSettings) {
        dataStore.updateDataAsync { prefs ->
            prefs[Keys.SOURCE_LOCK_ENABLED] = settings.enabled
            prefs[Keys.SOURCE_LOCK_FALLBACK_MODE] = settings.fallbackMode.ordinal
            prefs[Keys.SOURCE_LOCK_MAX_RETRIES] = settings.maxRetries
            prefs[Keys.SOURCE_LOCK_RETRY_DELAY_MS] = settings.retryDelayMs
            prefs[Keys.SOURCE_LOCK_PERSIST] = settings.persistAcrossSessions
            prefs[Keys.SOURCE_LOCK_NOTIFY_FALLBACK] = settings.notifyOnFallback
        }
    }

    override suspend fun clearCache() {
        dataStore.updateDataAsync { it[Keys.CACHE_SIZE] = 0L }
        context.cacheDir.deleteRecursively()
        context.getExternalCacheDir()?.deleteRecursively()
    }

    override fun getCacheSize(): Long {
        return dataStore.data.map { it[Keys.CACHE_SIZE] ?: 0L }.blockingFirst()
    }

    override suspend fun runBenchmarks() {
    }

    override suspend fun setDiskBufferSizeMb(sizeMb: Int) {
        dataStore.updateDataAsync { it[Keys.DISK_BUFFER_SIZE_MB] = sizeMb }
    }

    override suspend fun setDiskBufferReadAheadMb(sizeMb: Int) {
        dataStore.updateDataAsync { it[Keys.DISK_BUFFER_READ_AHEAD_MB] = sizeMb }
    }

    override suspend fun setDiskBufferLocation(location: String) {
        dataStore.updateDataAsync { it[Keys.DISK_BUFFER_LOCATION] = location }
    }

    override suspend fun setDiskBufferDeleteOnShutdown(enabled: Boolean) {
        dataStore.updateDataAsync { it[Keys.DISK_BUFFER_DELETE_ON_SHUTDOWN] = enabled }
    }

    override suspend fun setSeedWhileIdleEnabled(enabled: Boolean) {
        dataStore.updateDataAsync { it[Keys.SEED_WHILE_IDLE] = enabled }
    }

    override suspend fun setSequentialDownloadEnabled(enabled: Boolean) {
        dataStore.updateDataAsync { it[Keys.SEQUENTIAL_DOWNLOAD] = enabled }
    }

    override suspend fun setSeedRatioLimit(limit: Float) {
        dataStore.updateDataAsync { it[Keys.SEED_RATIO_LIMIT] = limit }
    }

    override suspend fun setGlobalDownloadLimit(kbps: Long) {
        dataStore.updateDataAsync { it[Keys.GLOBAL_DOWNLOAD_LIMIT_KBPS] = kbps }
    }

    override suspend fun setGlobalUploadLimit(kbps: Long) {
        dataStore.updateDataAsync { it[Keys.GLOBAL_UPLOAD_LIMIT_KBPS] = kbps }
    }

    override suspend fun setSourceLockEnabled(enabled: Boolean) {
        dataStore.updateDataAsync { it[Keys.SOURCE_LOCK_ENABLED] = enabled }
    }

    override suspend fun setSourceLockFallbackMode(mode: Int) {
        dataStore.updateDataAsync { it[Keys.SOURCE_LOCK_FALLBACK_MODE] = mode }
    }

    override suspend fun setSourceLockMaxRetries(retries: Int) {
        dataStore.updateDataAsync { it[Keys.SOURCE_LOCK_MAX_RETRIES] = retries }
    }

    override suspend fun setSourceLockRetryDelayMs(delay: Long) {
        dataStore.updateDataAsync { it[Keys.SOURCE_LOCK_RETRY_DELAY_MS] = delay }
    }

    override suspend fun setSourceLockPersist(persist: Boolean) {
        dataStore.updateDataAsync { it[Keys.SOURCE_LOCK_PERSIST] = persist }
    }

    override suspend fun setSourceLockNotifyFallback(notify: Boolean) {
        dataStore.updateDataAsync { it[Keys.SOURCE_LOCK_NOTIFY_FALLBACK] = notify }
    }

    override suspend fun clearAllSourceLocks() {
    }

    override suspend fun setAiUpscalingEnabled(enabled: Boolean) {
        dataStore.updateDataAsync { it[Keys.AI_UPSCALING] = enabled }
    }

    override suspend fun setFrameInterpolationEnabled(enabled: Boolean) {
        dataStore.updateDataAsync { it[Keys.FRAME_INTERPOLATION] = enabled }
    }

    override suspend fun setLowLatencyUpscalingEnabled(enabled: Boolean) {
        dataStore.updateDataAsync { it[Keys.LOW_LATENCY_UPSCALING] = enabled }
    }

    override suspend fun setVodCacheCompressionEnabled(enabled: Boolean) {
        dataStore.updateDataAsync { it[Keys.VOD_CACHE_COMPRESSION] = enabled }
    }

    override suspend fun setLiveWallpaperEnabled(enabled: Boolean) {
        dataStore.updateDataAsync { it[Keys.LIVE_WALLPAPER_ENABLED] = enabled }
    }

    override suspend fun setLiveWallpaperType(type: String) {
        dataStore.updateDataAsync { it[Keys.LIVE_WALLPAPER_TYPE] = type }
    }

    private fun formatCacheSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "${bytes} B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
            else -> "${bytes / (1024 * 1024 * 1024)} GB"
        }
    }

    private fun parseLanguages(stored: String): List<String> {
        return stored.split(",").filter { it.isNotBlank() }
    }
}