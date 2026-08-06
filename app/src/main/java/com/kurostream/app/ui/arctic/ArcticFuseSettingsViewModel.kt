// This file is part of KuroStream.
//
// ArcticFuseSettingsViewModel — ViewModel for the Arctic Fuse 3 settings page.
// Owns all KuroSettings mutations; every write goes to DataStore via
// KuroSettingsRepository so settings survive process death.
//
// SPDX-License-Identifier: GPL-3.0-only
package com.kurostream.app.ui.arctic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kurostream.data.settings.KuroSettings
import com.kurostream.data.settings.KuroSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ArcticFuseSettingsViewModel @Inject constructor(
    private val kuroSettingsRepository: KuroSettingsRepository,
) : ViewModel() {

    private val _settings = MutableStateFlow(KuroSettings())
    val settings: StateFlow<KuroSettings> = _settings.asStateFlow()

    init {
        viewModelScope.launch {
            _settings.value = kuroSettingsRepository.settings.first()
        }
    }

    // ── Appearance ─────────────────────────────────────────────────────────────

    fun setThemeMode(mode: com.kurostream.domain.repository.AppTheme) {
        update(kuroSettingsRepository::setThemeMode, mode)
    }

    fun setGlassCards(enabled: Boolean) {
        update(kuroSettingsRepository::setGlassCards, enabled)
    }

    fun setBlurEffects(enabled: Boolean) {
        update(kuroSettingsRepository::setBlurEffects, enabled)
    }

    fun setOledBlack(enabled: Boolean) {
        update(kuroSettingsRepository::setOledBlack, enabled)
    }

    fun setTagStyle(style: String) {
        update(kuroSettingsRepository::setTagStyle, style)
    }

    // ── Playback ───────────────────────────────────────────────────────────────

    fun setDefaultEngine(engine: String) {
        update(kuroSettingsRepository::setDefaultEngine, engine)
    }

    fun setDefaultQuality(quality: String) {
        update(kuroSettingsRepository::setDefaultQuality, quality)
    }

    fun setBufferSizeMs(ms: Int) {
        update(kuroSettingsRepository::setBufferSizeMs, ms)
    }

    fun setAutoPlayNext(enabled: Boolean) {
        update(kuroSettingsRepository::setAutoPlayNext, enabled)
    }

    fun setRefreshRateSwitching(enabled: Boolean) {
        update(kuroSettingsRepository::setRefreshRateSwitching, enabled)
    }

    // ── Video ──────────────────────────────────────────────────────────────────

    fun setUpscaleAlgorithm(algo: String) {
        update(kuroSettingsRepository::setUpscaleAlgorithm, algo)
    }

    fun setColorProfile(profile: String) {
        update(kuroSettingsRepository::setColorProfile, profile)
    }

    fun setContrastAdaptiveSharpening(enabled: Boolean) {
        update(kuroSettingsRepository::setContrastAdaptiveSharpening, enabled)
    }

    fun setFakeHdr(enabled: Boolean) {
        update(kuroSettingsRepository::setFakeHdr, enabled)
    }

    fun setOledMode(enabled: Boolean) {
        update(kuroSettingsRepository::setOledMode, enabled)
    }

    // ── Audio ──────────────────────────────────────────────────────────────────

    fun setPassthroughMode(mode: String) {
        update(kuroSettingsRepository::setPassthroughMode, mode)
    }

    fun setAudioDelayMs(ms: Int) {
        update(kuroSettingsRepository::setAudioDelayMs, ms)
    }

    fun setNightModeDrc(enabled: Boolean) {
        update(kuroSettingsRepository::setNightModeDrc, enabled)
    }

    fun setDialogueBoost(enabled: Boolean) {
        update(kuroSettingsRepository::setDialogueBoost, enabled)
    }

    // ── Subtitles ──────────────────────────────────────────────────────────────

    fun setSubtitleLanguagePriority(langs: List<String>) {
        update(kuroSettingsRepository::setSubtitleLanguagePriority, langs)
    }

    fun setSubtitleProviders(providers: List<String>) {
        update(kuroSettingsRepository::setSubtitleProviders, providers)
    }

    fun setSubtitleSize(size: Float) {
        update(kuroSettingsRepository::setSubtitleSize, size)
    }

    fun setSubtitleSyncOffset(ms: Int) {
        update(kuroSettingsRepository::setSubtitleSyncOffset, ms)
    }

    // ── Extensions ─────────────────────────────────────────────────────────────

    fun setEnabledExtensions(ids: Set<String>) {
        update(kuroSettingsRepository::setEnabledExtensions, ids)
    }

    fun setExtensionAutoUpdate(enabled: Boolean) {
        update(kuroSettingsRepository::setExtensionAutoUpdate, enabled)
    }

    fun setSandboxStrictMode(enabled: Boolean) {
        update(kuroSettingsRepository::setSandboxStrictMode, enabled)
    }

    // ── Network ────────────────────────────────────────────────────────────────

    fun setDohProvider(provider: String) {
        update(kuroSettingsRepository::setDohProvider, provider)
    }

    fun setCertificatePinning(enabled: Boolean) {
        update(kuroSettingsRepository::setCertificatePinning, enabled)
    }

    // ── Parental Controls ──────────────────────────────────────────────────────

    fun setKidsMode(enabled: Boolean) {
        update(kuroSettingsRepository::setKidsMode, enabled)
    }

    fun setParentalRatingLimit(rating: String) {
        update(kuroSettingsRepository::setParentalRatingLimit, rating)
    }

    // ── Accounts & Sync ────────────────────────────────────────────────────────

    fun setTraktSync(enabled: Boolean) {
        update(kuroSettingsRepository::setTraktSync, enabled)
    }

    fun setAnilistSync(enabled: Boolean) {
        update(kuroSettingsRepository::setAnilistSync, enabled)
    }

    fun setMalSync(enabled: Boolean) {
        update(kuroSettingsRepository::setMalSync, enabled)
    }

    // ── Hub ────────────────────────────────────────────────────────────────────

    fun setDefaultHub(hub: String) {
        update(kuroSettingsRepository::setDefaultHub, hub)
    }

    fun setMaxRows(rows: Int) {
        update(kuroSettingsRepository::setMaxRows, rows)
    }

    fun setHeroAutoScroll(enabled: Boolean) {
        update(kuroSettingsRepository::setHeroAutoScroll, enabled)
    }

    // ── Actions ────────────────────────────────────────────────────────────────

    fun resetDefaults() {
        viewModelScope.launch {
            kuroSettingsRepository.apply {
                setThemeMode(com.kurostream.domain.repository.AppTheme.DARK)
                setGlassCards(true)
                setBlurEffects(true)
                setOledBlack(false)
                setTagStyle("BOX")
                setDefaultEngine("Auto")
                setDefaultQuality("Auto")
                setBufferSizeMs(30000)
                setAutoPlayNext(true)
                setRefreshRateSwitching(false)
                setUpscaleAlgorithm("LANCZOS3")
                setColorProfile("NATURAL")
                setContrastAdaptiveSharpening(false)
                setFakeHdr(false)
                setOledMode(false)
                setPassthroughMode("AUTO")
                setAudioDelayMs(0)
                setNightModeDrc(false)
                setDialogueBoost(false)
                setSubtitleLanguagePriority(listOf("en", "ja"))
                setSubtitleProviders(listOf("opensubtitles", "subdl"))
                setSubtitleSize(1.0f)
                setSubtitleSyncOffset(0)
                setEnabledExtensions(emptySet())
                setExtensionAutoUpdate(true)
                setSandboxStrictMode(true)
                setDohProvider("Cloudflare")
                setCertificatePinning(false)
                setKidsMode(false)
                setParentalRatingLimit("PG-13")
                setTraktSync(false)
                setAnilistSync(false)
                setMalSync(false)
                setDefaultHub("Home")
                setMaxRows(5)
                setHeroAutoScroll(true)
            }
        }
    }

    // ── Private ────────────────────────────────────────────────────────────────

    private fun <T> update(writer: suspend (T) -> Unit, value: T) {
        viewModelScope.launch { writer(value) }
    }
}
