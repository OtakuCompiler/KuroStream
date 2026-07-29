package com.kurostream.app.baselineprofile

import androidx.benchmark.macro.MacrobenchmarkRule
import androidx.benchmark.macro.Metric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.compilation.CompilationMode
import androidx.benchmark.macro.junit4.MacrobenchmarkRunner
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kurostream.app.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StartupBenchmarks {

    @get:Rule
    val macrobenchmarkRule = MacrobenchmarkRule()

    @Test
    fun startupNoCompilation() = macrobenchmarkRule.measureRepeated(
        packageName = "com.kurostream.app",
        metrics = listOf(Metric.StartupTimeline),
        iterations = 10,
        startupMode = StartupMode.COLD,
        compilationMode = CompilationMode.None(),
        setupBlock = { pressHome() },
        measureBlock = {
            startActivityAndWait()
        }
    )

    @Test
    fun startupPartialCompilation() = macrobenchmarkRule.measureRepeated(
        packageName = "com.kurostream.app",
        metrics = listOf(Metric.StartupTimeline),
        iterations = 10,
        startupMode = StartupMode.COLD,
        compilationMode = CompilationMode.Partial(warmupIterations = 3),
        setupBlock = { pressHome() },
        measureBlock = {
            startActivityAndWait()
        }
    )

    @Test
    fun startupFullCompilation() = macrobenchmarkRule.measureRepeated(
        packageName = "com.kurostream.app",
        metrics = listOf(Metric.StartupTimeline),
        iterations = 10,
        startupMode = StartupMode.COLD,
        compilationMode = CompilationMode.Full(),
        setupBlock = { pressHome() },
        measureBlock = {
            startActivityAndWait()
        }
    )

    @Test
    fun startupWithBaselineProfile() = macrobenchmarkRule.measureRepeated(
        packageName = "com.kurostream.app",
        metrics = listOf(Metric.StartupTimeline),
        iterations = 10,
        startupMode = StartupMode.COLD,
        compilationMode = CompilationMode.Full(profileFile = "baseline-prof.txt"),
        setupBlock = { pressHome() },
        measureBlock = {
            startActivityAndWait()
        }
    )
}