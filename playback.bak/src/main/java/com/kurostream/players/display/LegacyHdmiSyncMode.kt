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
import android.view.Window
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import timber.log.Timber
import kotlin.math.abs

/**
 * Phase 29: Legacy HDMI Sync Mode (FINAL)
 */
class LegacyHdmiSyncMode(
    private val context: Context,
    private val window: Window,
    private val refreshRateSwitcher: RefreshRateSwitcher
) {
    private val _isEnabled = MutableStateFlow(false)
    val isEnabled: StateFlow<Boolean> = _isEnabled.asStateFlow()

    private val _currentPulldown = MutableStateFlow<PulldownMode>(PulldownMode.NONE)
    val currentPulldown: StateFlow<PulldownMode> = _currentPulldown.asStateFlow()

    private var contentFps: Float = 24f

    companion object {
        const val LEGACY_TARGET_RATE = 60f
        const val PULLDOWN_3_2 = "3:2"
        const val PULLDOWN_2_2_2_4 = "2:2:2:4"
        const val PULLDOWN_2_3_3_2 = "2:3:3:2"
    }

    enum class PulldownMode {
        NONE,
        PULLDOWN_3_2,
        PULLDOWN_2_2_2_4,
        PULLDOWN_2_3_3_2
    }

    fun enable(contentFrameRate: Float = 24f) {
        this.contentFps = contentFrameRate
        _isEnabled.value = true

        refreshRateSwitcher.switchToRate(LEGACY_TARGET_RATE)

        val pulldown = calculatePulldown(contentFrameRate)
        _currentPulldown.value = pulldown

        Timber.i("Legacy HDMI sync: ${LEGACY_TARGET_RATE}Hz with ${pulldown.name} for ${contentFrameRate}fps")
    }

    fun disable() {
        _isEnabled.value = false
        _currentPulldown.value = PulldownMode.NONE
        refreshRateSwitcher.restoreDefault()
        Timber.i("Legacy HDMI sync disabled")
    }

    fun toggle(contentFrameRate: Float = 24f) {
        if (_isEnabled.value) disable() else enable(contentFrameRate)
    }

    private fun calculatePulldown(contentFps: Float): PulldownMode {
        return when {
            abs(contentFps - 24f) < 0.1f -> PulldownMode.PULLDOWN_3_2
            abs(contentFps - 25f) < 0.1f -> PulldownMode.PULLDOWN_2_2_2_4
            abs(contentFps - 23.976f) < 0.01f -> PulldownMode.PULLDOWN_2_3_3_2
            abs(contentFps - 30f) < 0.1f -> PulldownMode.NONE
            else -> PulldownMode.PULLDOWN_3_2
        }
    }

    fun getFramePattern(): List<Int> {
        return when (_currentPulldown.value) {
            PulldownMode.NONE -> listOf(2, 2)
            PulldownMode.PULLDOWN_3_2 -> listOf(2, 3)
            PulldownMode.PULLDOWN_2_2_2_4 -> listOf(2, 2, 2, 4)
            PulldownMode.PULLDOWN_2_3_3_2 -> listOf(2, 3, 3, 2)
        }
    }

    fun getDisplayFrameIndex(sourceFrame: Int, patternPosition: Int): Int {
        val pattern = getFramePattern()
        val cycleLength = pattern.sum()
        val cycleFrame = sourceFrame % cycleLength

        var accumulated = 0
        for ((index, repeats) in pattern.withIndex()) {
            accumulated += repeats
            if (cycleFrame < accumulated) {
                return index
            }
        }
        return 0
    }

    fun getJudderEstimate(): Float {
        return when (_currentPulldown.value) {
            PulldownMode.NONE -> 0f
            PulldownMode.PULLDOWN_3_2 -> 0.4f
            PulldownMode.PULLDOWN_2_2_2_4 -> 0.3f
            PulldownMode.PULLDOWN_2_3_3_2 -> 0.25f
        }
    }

    fun isLegacyDevice(): Boolean {
        val modes = refreshRateSwitcher.currentMode.value
        return modes?.let {
            val has24Hz = refreshRateSwitcher.isSupported &&
                    refreshRateSwitcher.currentMode.value != null
            !has24Hz
        } ?: true
    }

    fun getModeDescription(): String {
        return when (_currentPulldown.value) {
            PulldownMode.NONE -> "Native ${contentFps}fps (no conversion)"
            PulldownMode.PULLDOWN_3_2 -> "60Hz with 3:2 pulldown (classic)"
            PulldownMode.PULLDOWN_2_2_2_4 -> "60Hz with 2:2:2:4 pulldown (smooth)"
            PulldownMode.PULLDOWN_2_3_3_2 -> "60Hz with 2:3:3:2 pulldown (advanced)"
        }
    }

    fun getFrameTimingAdjustment(): FrameTimingAdjustment {
        return when (_currentPulldown.value) {
            PulldownMode.NONE -> FrameTimingAdjustment(0f, 16.67f)
            PulldownMode.PULLDOWN_3_2 -> FrameTimingAdjustment(
                frameDelayMs = 0f,
                displayIntervalMs = 16.67f,
                pattern = listOf(33.33f, 33.33f, 50f, 50f, 50f)
            )
            PulldownMode.PULLDOWN_2_2_2_4 -> FrameTimingAdjustment(
                frameDelayMs = 0f,
                displayIntervalMs = 16.67f,
                pattern = listOf(33.33f, 33.33f, 33.33f, 33.33f, 66.67f, 66.67f, 66.67f, 66.67f)
            )
            PulldownMode.PULLDOWN_2_3_3_2 -> FrameTimingAdjustment(
                frameDelayMs = 0f,
                displayIntervalMs = 16.67f,
                pattern = listOf(33.33f, 33.33f, 50f, 50f, 50f, 50f, 50f, 50f, 33.33f, 33.33f)
            )
        }
    }

    data class FrameTimingAdjustment(
        val frameDelayMs: Float,
        val displayIntervalMs: Float,
        val pattern: List<Float> = emptyList()
    )
}
