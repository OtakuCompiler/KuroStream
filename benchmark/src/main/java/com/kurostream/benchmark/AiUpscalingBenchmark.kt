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

import android.os.SystemClock
import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kurostream.players.advanced.ai.SuperResolutionManager
import com.kurostream.players.advanced.ai.FrameInterpolationManager
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AiUpscalingBenchmark {

    @get:Rule
    val benchmarkRule = BenchmarkRule()

    @Test
    fun benchmark_superResolution_process() {
        val srManager = SuperResolutionManager(ApplicationProvider.getApplicationContext())

        benchmarkRule.measureRepeated {
            // Simulate processing a 1920x1080 frame to 3840x2160
            val inputFrame = android.graphics.Bitmap.createBitmap(1920, 1080, android.graphics.Bitmap.Config.ARGB_8888)
            val outputFrame = srManager.upscale(inputFrame, 2.0f)
            outputFrame?.recycle()
            inputFrame.recycle()
        }
    }

    @Test
    fun benchmark_frameInterpolation_process() {
        val fiManager = FrameInterpolationManager(ApplicationProvider.getApplicationContext())

        benchmarkRule.measureRepeated {
            val frame1 = android.graphics.Bitmap.createBitmap(1920, 1080, android.graphics.Bitmap.Config.ARGB_8888)
            val frame2 = android.graphics.Bitmap.createBitmap(1920, 1080, android.graphics.Bitmap.Config.ARGB_8888)
            val interpolated = fiManager.interpolate(frame1, frame2, 0.5f)
            interpolated?.recycle()
            frame1.recycle()
            frame2.recycle()
        }
    }

    @Test
    fun benchmark_combined_sr_fi_pipeline() {
        val srManager = SuperResolutionManager(ApplicationProvider.getApplicationContext())
        val fiManager = FrameInterpolationManager(ApplicationProvider.getApplicationContext())

        benchmarkRule.measureRepeated {
            val frame1 = android.graphics.Bitmap.createBitmap(1920, 1080, android.graphics.Bitmap.Config.ARGB_8888)
            val frame2 = android.graphics.Bitmap.createBitmap(1920, 1080, android.graphics.Bitmap.Config.ARGB_8888)

            // SR -> 2x
            val srFrame1 = srManager.upscale(frame1, 2.0f)
            val srFrame2 = srManager.upscale(frame2, 2.0f)

            // FI between SR frames
            val interpolated = fiManager.interpolate(srFrame1!!, srFrame2!!, 0.5f)

            interpolated?.recycle()
            srFrame1?.recycle()
            srFrame2?.recycle()
            frame1.recycle()
            frame2.recycle()
        }
    }
}