// This file is part of KuroStream.
//
// KuroVisionSettings — Persisted user-facing knobs. Backed by DataStore so
// they survive across app launches. All flows are cold and re-emit whenever
// any preference changes.
//
// SPDX-License-Identifier: GPL-3.0-only
package com.kurostream.playback.kurovision

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.kuroVisionDataStore by preferencesDataStore("kuro_vision")

/**
 * Strongly-typed access to KuroVision settings. Inject via Hilt or create
 * with the application context.
 */
class KuroVisionSettings(context: Context) {

    private val ds = context.kuroVisionDataStore

    val enabled: Flow<Boolean> = ds.data.map { it[KEY_ENABLED] ?: true }

    val qualityMode: Flow<KuroVisionQualityMode> = ds.data.map {
        val name = it[KEY_QUALITY_MODE] ?: KuroVisionQualityMode.CINEMA.name
        runCatching { KuroVisionQualityMode.valueOf(name) }.getOrDefault(KuroVisionQualityMode.CINEMA)
    }

    val upscaleAlgorithm: Flow<UpscaleAlgorithm> = ds.data.map {
        val name = it[KEY_UPSCALE_ALGO] ?: UpscaleAlgorithm.BICUBIC.name
        runCatching { UpscaleAlgorithm.valueOf(name) }.getOrDefault(UpscaleAlgorithm.BICUBIC)
    }

    val sharpening: Flow<Float> = ds.data.map { it[KEY_SHARPEN] ?: 0.25f }
    val denoise: Flow<Float> = ds.data.map { it[KEY_DENOISE] ?: 0.30f }
    val debanding: Flow<Float> = ds.data.map { it[KEY_DEBANDING] ?: 0.50f }
    val detailBoost: Flow<Float> = ds.data.map { it[KEY_DETAIL] ?: 0.40f }
    val fakeHdrIntensity: Flow<Float> = ds.data.map { it[KEY_HDR_INTENSITY] ?: 0.60f }
    val oledBlackIntensity: Flow<Float> = ds.data.map { it[KEY_OLED_INTENSITY] ?: 0.55f }
    val saturation: Flow<Float> = ds.data.map { it[KEY_SATURATION] ?: 1.10f }
    val contrast: Flow<Float> = ds.data.map { it[KEY_CONTRAST] ?: 1.05f }
    val frameInterpolation: Flow<Boolean> = ds.data.map { it[KEY_FRAME_INTERP] ?: false }
    val dolbyPassthrough: Flow<Boolean> = ds.data.map { it[KEY_DOLBY_PASSTHROUGH] ?: true }

    suspend fun setEnabled(value: Boolean) = ds.edit { it[KEY_ENABLED] = value }
    suspend fun setQualityMode(mode: KuroVisionQualityMode) = ds.edit { it[KEY_QUALITY_MODE] = mode.name }
    suspend fun setUpscaleAlgorithm(algo: UpscaleAlgorithm) = ds.edit { it[KEY_UPSCALE_ALGO] = algo.name }
    suspend fun setSharpening(v: Float) = ds.edit { it[KEY_SHARPEN] = v }
    suspend fun setDenoise(v: Float) = ds.edit { it[KEY_DENOISE] = v }
    suspend fun setDebanding(v: Float) = ds.edit { it[KEY_DEBANDING] = v }
    suspend fun setDetailBoost(v: Float) = ds.edit { it[KEY_DETAIL] = v }
    suspend fun setFakeHdrIntensity(v: Float) = ds.edit { it[KEY_HDR_INTENSITY] = v }
    suspend fun setOledBlackIntensity(v: Float) = ds.edit { it[KEY_OLED_INTENSITY] = v }
    suspend fun setSaturation(v: Float) = ds.edit { it[KEY_SATURATION] = v }
    suspend fun setContrast(v: Float) = ds.edit { it[KEY_CONTRAST] = v }
    suspend fun setFrameInterpolation(v: Boolean) = ds.edit { it[KEY_FRAME_INTERP] = v }
    suspend fun setDolbyPassthrough(v: Boolean) = ds.edit { it[KEY_DOLBY_PASSTHROUGH] = v }

    companion object {
        val KEY_ENABLED = booleanPreferencesKey("kv_enabled")
        val KEY_QUALITY_MODE = stringPreferencesKey("kv_quality_mode")
        val KEY_UPSCALE_ALGO = stringPreferencesKey("kv_upscale_algo")
        val KEY_SHARPEN = floatPreferencesKey("kv_sharpen")
        val KEY_DENOISE = floatPreferencesKey("kv_denoise")
        val KEY_DEBANDING = floatPreferencesKey("kv_debanding")
        val KEY_DETAIL = floatPreferencesKey("kv_detail")
        val KEY_HDR_INTENSITY = floatPreferencesKey("kv_hdr_intensity")
        val KEY_OLED_INTENSITY = floatPreferencesKey("kv_oled_intensity")
        val KEY_SATURATION = floatPreferencesKey("kv_saturation")
        val KEY_CONTRAST = floatPreferencesKey("kv_contrast")
        val KEY_FRAME_INTERP = booleanPreferencesKey("kv_frame_interp")
        val KEY_DOLBY_PASSTHROUGH = booleanPreferencesKey("kv_dolby_passthrough")
    }
}
