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

package com.kurostream.players.display

import android.content.Context
import android.hardware.display.DisplayManager
import android.os.Build
import android.view.Display
import android.view.Window
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import timber.log.Timber
import kotlin.math.abs

/**
 * Phase 28: Refresh Rate Switching (FINAL)
 */
class RefreshRateSwitcher(
    private val context: Context,
    private val window: Window
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager

    private val supportedModes by lazy { getSupportedModes() }

    private val _currentMode = MutableStateFlow<DisplayMode?>(null)
    val currentMode: StateFlow<DisplayMode?> = _currentMode.asStateFlow()

    private val _contentFrameRate = MutableStateFlow(0f)
    val contentFrameRate: StateFlow<Float> = _contentFrameRate.asStateFlow()

    val isSupported: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && supportedModes.isNotEmpty()

    private var monitorJob: Job? = null

    companion object {
        val COMMON_RATES = listOf(23.976f, 24f, 25f, 29.97f, 30f, 50f, 59.94f, 60f, 120f)
        const val MATCH_TOLERANCE = 0.1f
        const val MIN_SWITCH_INTERVAL_MS = 5_000L
        const val LG_23_976_MODE = "23.976Hz"
        const val LG_24P_MODE = "24p"
    }

    fun startAutoSwitch(player: com.kurostream.players.core.PlayerInterface) {
        if (!isSupported) {
            Timber.w("Refresh rate switching not supported")
            return
        }

        monitorJob?.cancel()
        monitorJob = scope.launch {
            player.diagnostics.collect { diagnostics ->
                val contentFps = diagnostics.contentFrameRate
                if (contentFps > 0 && abs(contentFps - _contentFrameRate.value) > 0.1f) {
                    _contentFrameRate.value = contentFps
                    switchToOptimalRate(contentFps)
                }
            }
        }
    }

    fun stopAutoSwitch() {
        monitorJob?.cancel()
        monitorJob = null
    }

    fun switchToRate(targetRate: Float): Boolean {
        if (!isSupported) return false
        val bestMode = findBestMode(targetRate) ?: return false
        return applyMode(bestMode)
    }

    private fun switchToOptimalRate(contentFps: Float) {
        val bestMode = findBestMode(contentFps) ?: return
        if (_currentMode.value?.refreshRate == bestMode.refreshRate) {
            Timber.d("Already at optimal rate: ${bestMode.refreshRate}Hz")
            return
        }
        applyMode(bestMode)
    }

    private fun findBestMode(contentFps: Float): DisplayMode? {
        if (supportedModes.isEmpty()) return null

        val exactMatch = supportedModes.find { abs(it.refreshRate - contentFps) < MATCH_TOLERANCE }
        if (exactMatch != null) return exactMatch

        val integerMultiples = supportedModes.filter { mode ->
            val ratio = mode.refreshRate / contentFps
            abs(ratio - ratio.toInt()) < 0.05f && ratio >= 1f
        }
        if (integerMultiples.isNotEmpty()) {
            return integerMultiples.minByOrNull { it.refreshRate }
        }

        val higherRates = supportedModes.filter { it.refreshRate >= contentFps }
        if (higherRates.isNotEmpty()) {
            return higherRates.minByOrNull { abs(it.refreshRate - contentFps) }
        }

        return supportedModes.maxByOrNull { it.refreshRate }
    }

    private fun applyMode(mode: DisplayMode): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return false

        try {
            val params = window.attributes

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                params.preferredRefreshRate = mode.refreshRate
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val display = displayManager.getDisplay(Display.DEFAULT_DISPLAY)
                val androidMode = display?.supportedModes?.find {
                    abs(it.refreshRate - mode.refreshRate) < MATCH_TOLERANCE
                }
                if (androidMode != null) {
                    params.preferredDisplayModeId = androidMode.modeId
                }
            }

            window.attributes = params
            _currentMode.value = mode

            Timber.i("Switched to ${mode.refreshRate}Hz")
            return true
        } catch (e: Exception) {
            Timber.e(e, "Failed to switch refresh rate")
            return false
        }
    }

    private fun getSupportedModes(): List<DisplayMode> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return emptyList()

        val display = displayManager.getDisplay(Display.DEFAULT_DISPLAY) ?: return emptyList()

        return display.supportedModes.map { mode ->
            DisplayMode(
                modeId = mode.modeId,
                refreshRate = mode.refreshRate,
                width = mode.physicalWidth,
                height = mode.physicalHeight,
                name = "${mode.refreshRate}Hz @ ${mode.physicalWidth}x${mode.physicalHeight}"
            )
        }.distinctBy { it.refreshRate }
    }

    fun restoreDefault() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val params = window.attributes
            params.preferredRefreshRate = 0f
            window.attributes = params
        }
        _currentMode.value = null
        Timber.i("Restored default refresh rate")
    }

    fun isOptimalForContent(contentFps: Float): Boolean {
        val current = _currentMode.value?.refreshRate ?: return false
        val ratio = current / contentFps
        return abs(ratio - ratio.toInt()) < 0.05f || abs(current - contentFps) < MATCH_TOLERANCE
    }

    fun getRecommendedRate(contentFps: Float): Float? {
        return findBestMode(contentFps)?.refreshRate
    }

    data class DisplayMode(
        val modeId: Int,
        val refreshRate: Float,
        val width: Int,
        val height: Int,
        val name: String
    )
}
