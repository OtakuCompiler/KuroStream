package com.kurostream.app.baselineprofile

import androidx.benchmark.macro.BaselineProfileRule
import androidx.benchmark.macro.compilation.CompilationMode
import androidx.benchmark.macro.junit4.BaselineProfileRunner
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kurostream.app.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {

    @get:Rule
    val baselineProfileRule = BaselineProfileRule(
        packageName = "com.kurostream.app",
        profileBlock = {
            // Startup - critical user journey
            startup(includeAllStartupCpuData = true) {
                pressHome()
                launchActivity()
            }

            // Navigation to Home screen
            navigateToHome()

            // Navigate to Search
            navigateToSearch()

            // Navigate to Details
            navigateToDetails()
        }
    )

    @Test
    fun generateBaselineProfile() {
        // This test generates the baseline profile
        // Run with: ./gradlew :app:generateBaselineProfile
    }

    private fun BaselineProfileRule.ProfileBlock.startup(includeAllStartupCpuData: Boolean) {
        measureRepeated(packageName = "com.kurostream.app", metrics = listOf(), iterations = 10) {
            pressHome()
            launchActivity()
            if (includeAllStartupCpuData) {
                // Include all CPU data for startup
            }
        }
    }

    private fun BaselineProfileRule.ProfileBlock.navigateToHome() {
        measureRepeated(packageName = "com.kurostream.app", iterations = 5) {
            // Navigate to home - already there after startup
            waitForComposeIdle()
        }
    }

    private fun BaselineProfileRule.ProfileBlock.navigateToSearch() {
        measureRepeated(packageName = "com.kurostream.app", iterations = 5) {
            // Press DPAD_RIGHT to navigate to search tab
            pressMenu()
            pressDpadRight()
            pressDpadRight()
            pressDpadCenter()
            waitForComposeIdle()
        }
    }

    private fun BaselineProfileRule.ProfileBlock.navigateToDetails() {
        measureRepeated(packageName = "com.kurostream.app", iterations = 5) {
            // Navigate to a detail screen
            pressDpadDown()
            pressDpadCenter()
            waitForComposeIdle()
            pressBack()
        }
    }
}