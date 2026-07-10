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

package com.kurostream.players.advanced.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Performance settings with super-resolution toggle.
 */
class PerformanceSettings(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _superResolutionEnabled = MutableStateFlow(prefs.getBoolean(KEY_SR_ENABLED, false))
    val superResolutionEnabled: StateFlow<Boolean> = _superResolutionEnabled.asStateFlow()

    private val _frameInterpolationEnabled = MutableStateFlow(prefs.getBoolean(KEY_FI_ENABLED, false))
    val frameInterpolationEnabled: StateFlow<Boolean> = _frameInterpolationEnabled.asStateFlow()

    private val _targetQuality = MutableStateFlow(prefs.getString(KEY_TARGET_QUALITY, "balanced") ?: "balanced")
    val targetQuality: StateFlow<String> = _targetQuality.asStateFlow()

    private val _nnapiEnabled = MutableStateFlow(prefs.getBoolean(KEY_NNAPI_ENABLED, true))
    val nnapiEnabled: StateFlow<Boolean> = _nnapiEnabled.asStateFlow()

    var superResolutionEnabledValue: Boolean
        get() = _superResolutionEnabled.value
        set(value) {
            _superResolutionEnabled.value = value
            prefs.edit { putBoolean(KEY_SR_ENABLED, value) }
        }

    var frameInterpolationEnabledValue: Boolean
        get() = _frameInterpolationEnabled.value
        set(value) {
            _frameInterpolationEnabled.value = value
            prefs.edit { putBoolean(KEY_FI_ENABLED, value) }
        }

    var nnapiEnabledValue: Boolean
        get() = _nnapiEnabled.value
        set(value) {
            _nnapiEnabled.value = value
            prefs.edit { putBoolean(KEY_NNAPI_ENABLED, value) }
        }

    companion object {
        private const val PREFS_NAME = "kurostream_performance"
        private const val KEY_SR_ENABLED = "super_resolution_enabled"
        private const val KEY_FI_ENABLED = "frame_interpolation_enabled"
        private const val KEY_TARGET_QUALITY = "target_quality"
        private const val KEY_NNAPI_ENABLED = "nnapi_enabled"
    }
}
