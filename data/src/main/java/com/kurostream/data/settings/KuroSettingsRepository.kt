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

package com.kurostream.data.settings

import com.kurostream.data.local.preferences.SettingsDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KuroSettingsRepository @Inject constructor(
    private val settingsDataStore: SettingsDataStore,
) {

    val settings: Flow<KuroSettings> = settingsDataStore.data.map { prefs ->
        KuroSettings(
            themeMode = parseTheme(prefs[K.THEME_MODE]),
            glassCards = prefs[K.GLASS_CARDS] ?: true,
            blurEffects = prefs[K.BLUR_EFFECTS] ?: true,
            oledBlack = prefs[K.OLED_BLACK] ?: false,
            tagStyle = prefs[K.TAG_STYLE] ?: "BOX",
            defaultEngine = prefs[K.DEFAULT_ENGINE] ?: "Auto",
            defaultQuality = prefs[K.DEFAULT_QUALITY] ?: "Auto",
            bufferSizeMs = prefs[K.BUFFER_SIZE_MS] ?: 30000,
            autoPlayNext = prefs[K.AUTO_PLAY_NEXT] ?: true,
            refreshRateSwitching = prefs[K.REFRESH_RATE_SWITCHING] ?: false,
            upscaleAlgorithm = prefs[K.UPSCALE_ALGORITHM] ?: "LANCZOS3",
            colorProfile = prefs[K.COLOR_PROFILE] ?: "NATURAL",
            contrastAdaptiveSharpening = prefs[K.CONTRAST_ADAPTIVE_SHARPENING] ?: false,
            fakeHdr = prefs[K.FAKE_HDR] ?: false,
            oledMode = prefs[K.OLED_MODE] ?: false,
            passthroughMode = prefs[K.PASSTHROUGH_MODE] ?: "AUTO",
            audioDelayMs = prefs[K.AUDIO_DELAY_MS] ?: 0,
            nightModeDrc = prefs[K.NIGHT_MODE_DRC] ?: false,
            dialogueBoost = prefs[K.DIALOGUE_BOOST] ?: false,
            subtitleLanguagePriority = (prefs[K.SUBTITLE_LANG_PRIORITY] ?: "en,ja").split(",").filter { it.isNotBlank() },
            subtitleProviders = (prefs[K.SUBTITLE_PROVIDERS] ?: "opensubtitles,subdl").split(",").filter { it.isNotBlank() },
            subtitleSize = prefs[K.SUBTITLE_SIZE] ?: 1.0f,
            subtitleSyncOffset = prefs[K.SUBTITLE_SYNC_OFFSET] ?: 0,
            enabledExtensions = (prefs[K.ENABLED_EXTENSIONS] ?: "").split(",").filter { it.isNotBlank() }.toSet(),
            extensionAutoUpdate = prefs[K.EXTENSION_AUTO_UPDATE] ?: true,
            sandboxStrictMode = prefs[K.SANDBOX_STRICT_MODE] ?: true,
            dohProvider = prefs[K.DOH_PROVIDER] ?: "Cloudflare",
            certificatePinning = prefs[K.CERTIFICATE_PINNING] ?: false,
            kidsMode = prefs[K.KIDS_MODE] ?: false,
            pinHash = prefs[K.PIN_HASH],
            parentalRatingLimit = prefs[K.PARENTAL_RATING_LIMIT] ?: "PG-13",
            traktSync = prefs[K.TRAKT_SYNC] ?: false,
            anilistSync = prefs[K.ANILIST_SYNC] ?: false,
            malSync = prefs[K.MAL_SYNC] ?: false,
            defaultHub = prefs[K.DEFAULT_HUB] ?: "Home",
            maxRows = prefs[K.MAX_ROWS] ?: 5,
            heroAutoScroll = prefs[K.HERO_AUTO_SCROLL] ?: true,
        )
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private fun parseTheme(raw: String?): com.kurostream.domain.repository.AppTheme =
        when (raw) {
            "LIGHT" -> com.kurostream.domain.repository.AppTheme.LIGHT
            "OLED"  -> com.kurostream.domain.repository.AppTheme.OLED
            "DARK"  -> com.kurostream.domain.repository.AppTheme.DARK
            else    -> com.kurostream.domain.repository.AppTheme.SYSTEM
        }

    // ── Appearance ────────────────────────────────────────────────────────────

    suspend fun setThemeMode(theme: com.kurostream.domain.repository.AppTheme) {
        settingsDataStore.setString(K.THEME_MODE, theme.name)
    }

    suspend fun setGlassCards(enabled: Boolean) {
        settingsDataStore.setBoolean(K.GLASS_CARDS, enabled)
    }

    suspend fun setBlurEffects(enabled: Boolean) {
        settingsDataStore.setBoolean(K.BLUR_EFFECTS, enabled)
    }

    suspend fun setOledBlack(enabled: Boolean) {
        settingsDataStore.setBoolean(K.OLED_BLACK, enabled)
    }

    suspend fun setTagStyle(style: String) {
        settingsDataStore.setString(K.TAG_STYLE, style)
    }

    // ── Playback ──────────────────────────────────────────────────────────────

    suspend fun setDefaultEngine(engine: String) {
        settingsDataStore.setString(K.DEFAULT_ENGINE, engine)
    }

    suspend fun setDefaultQuality(quality: String) {
        settingsDataStore.setString(K.DEFAULT_QUALITY, quality)
    }

    suspend fun setBufferSizeMs(ms: Int) {
        settingsDataStore.setInt(K.BUFFER_SIZE_MS, ms)
    }

    suspend fun setAutoPlayNext(enabled: Boolean) {
        settingsDataStore.setBoolean(K.AUTO_PLAY_NEXT, enabled)
    }

    suspend fun setRefreshRateSwitching(enabled: Boolean) {
        settingsDataStore.setBoolean(K.REFRESH_RATE_SWITCHING, enabled)
    }

    // ── Video ─────────────────────────────────────────────────────────────────

    suspend fun setUpscaleAlgorithm(algo: String) {
        settingsDataStore.setString(K.UPSCALE_ALGORITHM, algo)
    }

    suspend fun setColorProfile(profile: String) {
        settingsDataStore.setString(K.COLOR_PROFILE, profile)
    }

    suspend fun setContrastAdaptiveSharpening(enabled: Boolean) {
        settingsDataStore.setBoolean(K.CONTRAST_ADAPTIVE_SHARPENING, enabled)
    }

    suspend fun setFakeHdr(enabled: Boolean) {
        settingsDataStore.setBoolean(K.FAKE_HDR, enabled)
    }

    suspend fun setOledMode(enabled: Boolean) {
        settingsDataStore.setBoolean(K.OLED_MODE, enabled)
    }

    // ── Audio ─────────────────────────────────────────────────────────────────

    suspend fun setPassthroughMode(mode: String) {
        settingsDataStore.setString(K.PASSTHROUGH_MODE, mode)
    }

    suspend fun setAudioDelayMs(ms: Int) {
        settingsDataStore.setInt(K.AUDIO_DELAY_MS, ms)
    }

    suspend fun setNightModeDrc(enabled: Boolean) {
        settingsDataStore.setBoolean(K.NIGHT_MODE_DRC, enabled)
    }

    suspend fun setDialogueBoost(enabled: Boolean) {
        settingsDataStore.setBoolean(K.DIALOGUE_BOOST, enabled)
    }

    // ── Subtitles ─────────────────────────────────────────────────────────────

    suspend fun setSubtitleLanguagePriority(langs: List<String>) {
        settingsDataStore.setString(K.SUBTITLE_LANG_PRIORITY, langs.joinToString(","))
    }

    suspend fun setSubtitleProviders(providers: List<String>) {
        settingsDataStore.setString(K.SUBTITLE_PROVIDERS, providers.joinToString(","))
    }

    suspend fun setSubtitleSize(size: Float) {
        settingsDataStore.setFloat(K.SUBTITLE_SIZE, size)
    }

    suspend fun setSubtitleSyncOffset(ms: Int) {
        settingsDataStore.setInt(K.SUBTITLE_SYNC_OFFSET, ms)
    }

    // ── Extensions ─────────────────────────────────────────────────────────────

    suspend fun setEnabledExtensions(ids: Set<String>) {
        settingsDataStore.setString(K.ENABLED_EXTENSIONS, ids.joinToString(","))
    }

    suspend fun setExtensionAutoUpdate(enabled: Boolean) {
        settingsDataStore.setBoolean(K.EXTENSION_AUTO_UPDATE, enabled)
    }

    suspend fun setSandboxStrictMode(enabled: Boolean) {
        settingsDataStore.setBoolean(K.SANDBOX_STRICT_MODE, enabled)
    }

    // ── Network ────────────────────────────────────────────────────────────────

    suspend fun setDohProvider(provider: String) {
        settingsDataStore.setString(K.DOH_PROVIDER, provider)
    }

    suspend fun setCertificatePinning(enabled: Boolean) {
        settingsDataStore.setBoolean(K.CERTIFICATE_PINNING, enabled)
    }

    // ── Parental Controls ──────────────────────────────────────────────────────

    suspend fun setKidsMode(enabled: Boolean) {
        settingsDataStore.setBoolean(K.KIDS_MODE, enabled)
    }

    suspend fun setParentalRatingLimit(rating: String) {
        settingsDataStore.setString(K.PARENTAL_RATING_LIMIT, rating)
    }

    // ── Accounts & Sync ────────────────────────────────────────────────────────

    suspend fun setTraktSync(enabled: Boolean) {
        settingsDataStore.setBoolean(K.TRAKT_SYNC, enabled)
    }

    suspend fun setAnilistSync(enabled: Boolean) {
        settingsDataStore.setBoolean(K.ANILIST_SYNC, enabled)
    }

    suspend fun setMalSync(enabled: Boolean) {
        settingsDataStore.setBoolean(K.MAL_SYNC, enabled)
    }

    // ── Hub ───────────────────────────────────────────────────────────────────

    suspend fun setDefaultHub(hub: String) {
        settingsDataStore.setString(K.DEFAULT_HUB, hub)
    }

    suspend fun setMaxRows(rows: Int) {
        settingsDataStore.setInt(K.MAX_ROWS, rows)
    }

    suspend fun setHeroAutoScroll(enabled: Boolean) {
        settingsDataStore.setBoolean(K.HERO_AUTO_SCROLL, enabled)
    }

    // ── Settings DataStore key constants ───────────────────────────────────────
    //
    // Kept in one place so callers cannot silently drift to a new string value
    // and silently produce a brand-new Preferences.Key at runtime (the keyCache
    // in SettingsDataStoreImpl would hide the mistake).

    private object K {
        val THEME_MODE                = stringPreferencesKey("af3_theme_mode")
        val GLASS_CARDS               = booleanPreferencesKey("af3_glass_cards")
        val BLUR_EFFECTS              = booleanPreferencesKey("af3_blur_effects")
        val OLED_BLACK                = booleanPreferencesKey("af3_oled_black")
        val TAG_STYLE                 = stringPreferencesKey("af3_tag_style")
        val DEFAULT_ENGINE            = stringPreferencesKey("af3_default_engine")
        val DEFAULT_QUALITY           = stringPreferencesKey("af3_default_quality")
        val BUFFER_SIZE_MS            = intPreferencesKey("af3_buffer_size_ms")
        val AUTO_PLAY_NEXT            = booleanPreferencesKey("af3_auto_play_next")
        val REFRESH_RATE_SWITCHING    = booleanPreferencesKey("af3_refresh_rate_switching")
        val UPSCALE_ALGORITHM         = stringPreferencesKey("af3_upscale_algorithm")
        val COLOR_PROFILE             = stringPreferencesKey("af3_color_profile")
        val CONTRAST_ADAPTIVE_SHARPENING = booleanPreferencesKey("af3_contrast_adaptive_sharpening")
        val FAKE_HDR                  = booleanPreferencesKey("af3_fake_hdr")
        val OLED_MODE                 = booleanPreferencesKey("af3_oled_mode")
        val PASSTHROUGH_MODE          = stringPreferencesKey("af3_passthrough_mode")
        val AUDIO_DELAY_MS            = intPreferencesKey("af3_audio_delay_ms")
        val NIGHT_MODE_DRC            = booleanPreferencesKey("af3_night_mode_drc")
        val DIALOGUE_BOOST            = booleanPreferencesKey("af3_dialogue_boost")
        val SUBTITLE_LANG_PRIORITY    = stringPreferencesKey("af3_subtitle_lang_priority")
        val SUBTITLE_PROVIDERS        = stringPreferencesKey("af3_subtitle_providers")
        val SUBTITLE_SIZE             = floatPreferencesKey("af3_subtitle_size")
        val SUBTITLE_SYNC_OFFSET      = intPreferencesKey("af3_subtitle_sync_offset")
        val ENABLED_EXTENSIONS        = stringPreferencesKey("af3_enabled_extensions")
        val EXTENSION_AUTO_UPDATE     = booleanPreferencesKey("af3_extension_auto_update")
        val SANDBOX_STRICT_MODE       = booleanPreferencesKey("af3_sandbox_strict_mode")
        val DOH_PROVIDER              = stringPreferencesKey("af3_doh_provider")
        val CERTIFICATE_PINNING       = booleanPreferencesKey("af3_certificate_pinning")
        val KIDS_MODE                 = booleanPreferencesKey("af3_kids_mode")
        val PIN_HASH                  = stringPreferencesKey("af3_pin_hash")
        val PARENTAL_RATING_LIMIT     = stringPreferencesKey("af3_parental_rating_limit")
        val TRAKT_SYNC                = booleanPreferencesKey("af3_trakt_sync")
        val ANILIST_SYNC              = booleanPreferencesKey("af3_anilist_sync")
        val MAL_SYNC                  = booleanPreferencesKey("af3_mal_sync")
        val DEFAULT_HUB               = stringPreferencesKey("af3_default_hub")
        val MAX_ROWS                  = intPreferencesKey("af3_max_rows")
        val HERO_AUTO_SCROLL          = booleanPreferencesKey("af3_hero_auto_scroll")
    }
}
