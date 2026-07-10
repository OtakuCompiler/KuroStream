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

package com.kurostream.benchmark

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.measureRepeated
import androidx.benchmark.macro.perfettoTrace
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Test

class StartupBenchmark {
    @Test
    fun startupCold() = benchmarkRule.measureRepeated(
        packageName = "com.kurostream.app",
        metrics = listOf(StartupTimingMetric()),
        iterations = 10,
        startupMode = StartupMode.COLD,
        setupBlock = { scope ->
            scope.perfettoTrace("StartupTrace") {
                val device = UiDevice.getInstance(scope.context)
                device.waitForIdle()
            }
        },
        measureBlock = { scope ->
            scope.perfettoTrace("StartupMeasure") {
                scope.startActivityAndWait()
            }
        }
    )

    @Test
    fun startupWarm() = benchmarkRule.measureRepeated(
        packageName = "com.kurostream.app",
        metrics = listOf(StartupTimingMetric()),
        iterations = 10,
        startupMode = StartupMode.WARM,
        measureBlock = { scope ->
            scope.perfettoTrace("StartupMeasureWarm") {
                scope.startActivityAndWait()
            }
        }
    )

    @Test
    fun startupHot() = benchmarkRule.measureRepeated(
        packageName = "com.kurostream.app",
        metrics = listOf(StartupTimingMetric()),
        iterations = 10,
        startupMode = StartupMode.HOT,
        measureBlock = { scope ->
            scope.perfettoTrace("StartupMeasureHot") {
                scope.startActivityAndWait()
            }
        }
    )

    @Test
    fun homeScreenScroll() = benchmarkRule.measureRepeated(
        packageName = "com.kurostream.app",
        metrics = listOf(FrameTimingMetric()),
        iterations = 10,
        setupBlock = { scope ->
            scope.startActivityAndWait()
            // Wait for home screen to load
            val device = UiDevice.getInstance(scope.context)
            device.wait(Until.hasObject(By.res("com.kurostream.app:id/home_screen")), 5000)
        },
        measureBlock = { scope ->
            val device = UiDevice.getInstance(scope.context)
            device.swipe(540, 1500, 540, 500, 20) // Scroll down
            scope.perfettoTrace("HomeScroll") {
                // Measure frame timing during scroll
            }
        }
    )

    @Test
    fun playbackStartLatency() = benchmarkRule.measureRepeated(
        packageName = "com.kurostream.app",
        metrics = listOf(StartupTimingMetric()),
        iterations = 10,
        setupBlock = { scope ->
            scope.startActivityAndWait()
            // Navigate to an episode and start playback
            val device = UiDevice.getInstance(scope.context)
            device.wait(Until.hasObject(By.text("Play")), 5000)
            device.findObject(By.text("Play")).click()
        },
        measureBlock = { scope ->
            scope.perfettoTrace("PlaybackStart") {
                // Measure time from click to first frame
            }
        }
    )
}