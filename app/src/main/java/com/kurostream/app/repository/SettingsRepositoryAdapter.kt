package com.kurostream.app.repository

import com.kurostream.domain.repository.AppTheme
import com.kurostream.domain.repository.SettingsRepository as DomainSettingsRepo
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Adapter that bridges the domain SettingsRepository to the app's expected API.
 * The app expects a Settings data class; the domain exposes individual flows.
 */
@Singleton
class SettingsRepositoryAdapter @Inject constructor(
    private val domainRepo: DomainSettingsRepo
) {
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
        val diskBufferSizeMb: Int = 200,
        val diskBufferReadAheadMb: Int = 4,
        val diskBufferLocation: String = "internal",
        val diskBufferDeleteOnShutdown: Boolean = false,
        val aiUpscalingEnabled: Boolean = false,
        val frameInterpolationEnabled: Boolean = false,
        val lowLatencyUpscalingEnabled: Boolean = false,
        val vodCacheCompressionEnabled: Boolean = true,
        val skinName: String = "ARCTIC_FUSE",
        val subtitleFontSize: Float = 24f,
        val subtitleFontColorHex: String = "#FFFFFF",
        val subtitleBgColorHex: String = "#80000000",
        val subtitleEnabled: Boolean = true,
    )

    private suspend fun <T> safeFirst(flow: kotlinx.coroutines.flow.Flow<T>, fallback: T): T =
        try { flow.first() } catch (_: Exception) { fallback }

    suspend fun getSettings(): Settings {
        val subtitle = domainRepo.getPlayerSubtitleSettings()
        return Settings(
            skinName = safeFirst(domainRepo.observeSkinName(), "ARCTIC_FUSE"),
            reduceMotionEnabled = safeFirst(domainRepo.observeReduceMotionEnabled(), false),
            skipIntroEnabled = safeFirst(domainRepo.observeSkipIntroEnabled(), true),
            subtitleFontSize = subtitle.fontSize,
            subtitleFontColorHex = subtitle.fontColorHex,
            subtitleBgColorHex = subtitle.bgColorHex,
            subtitleEnabled = subtitle.enabled,
            autoPlayNextEnabled = true,
            hardwareAccelerationEnabled = true,
            backgroundPlaybackEnabled = false,
            debugOverlayEnabled = false,
            cacheSizeFormatted = "0 MB",
            preferredAudioLanguages = emptyList(),
            preferredSubtitleLanguages = emptyList(),
            highContrastEnabled = false,
            focusHighlightEnabled = true,
            diskBufferSizeMb = 200,
            diskBufferReadAheadMb = 4,
            diskBufferLocation = "internal",
            diskBufferDeleteOnShutdown = false,
            aiUpscalingEnabled = false,
            frameInterpolationEnabled = false,
            lowLatencyUpscalingEnabled = false,
            vodCacheCompressionEnabled = true,
        )
    }
}
