package com.kurostream.benchmark

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {

    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    @Test
    fun generateBaselineProfile() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        baselineProfileRule.collect(
            packageName = context.packageName,
            metrics = listOf(StartupTimingMetric(), FrameTimingMetric()),
            compilationMode = CompilationMode.Full(),
            startupMode = StartupMode.COLD
        ) {
            pressHome()
            startActivityAndWait()
        }
    }
}
