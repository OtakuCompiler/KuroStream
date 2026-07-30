package com.kurostream.players.advanced.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.fakeHdrDataStore by preferencesDataStore("fake_hdr")

class FakeHdrSettings(context: Context) {
    private val ds = context.fakeHdrDataStore

    val enabled: Flow<Boolean> = ds.data.map { it[KEY_ENABLED] ?: false }
    val intensity: Flow<Float> = ds.data.map { it[KEY_INTENSITY] ?: 0.6f }
    val contrast: Flow<Float> = ds.data.map { it[KEY_CONTRAST] ?: 1.15f }
    val saturation: Flow<Float> = ds.data.map { it[KEY_SATURATION] ?: 1.25f }

    companion object {
        val KEY_ENABLED = booleanPreferencesKey("fake_hdr_enabled")
        val KEY_INTENSITY = floatPreferencesKey("fake_hdr_intensity")
        val KEY_CONTRAST = floatPreferencesKey("fake_hdr_contrast")
        val KEY_SATURATION = floatPreferencesKey("fake_hdr_saturation")
    }
}
