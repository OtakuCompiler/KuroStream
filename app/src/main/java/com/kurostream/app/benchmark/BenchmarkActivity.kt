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

package com.kurostream.app.benchmark

import android.app.Activity
import android.os.Bundle
import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.AndroidBenchmarkRunner
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kurostream.app.ui.screens.details.DetailsScreen
import com.kurostream.app.ui.screens.details.DetailsViewModel
import com.kurostream.app.ui.screens.home.HomeScreen
import com.kurostream.app.ui.screens.home.HomeViewModel
import com.kurostream.app.ui.screens.player.PlayerScreen
import com.kurostream.app.ui.screens.player.PlayerViewModel
import com.kurostream.app.ui.screens.search.SearchScreen
import com.kurostream.app.ui.screens.settings.SettingsScreen
import com.kurostream.app.ui.screens.settings.SettingsViewModel
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.viewmodel.ViewModelProvider
import androidx.test.core.app.ApplicationProvider
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BenchmarkActivity : Activity() {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun runAllBenchmarks() {
        // Home screen startup
        measureStartup("HomeScreen") {
            composeRule.setContent {
                HomeScreen(
                    onMediaClick = {},
                    onSearchClick = {},
                    onDownloadsClick = {},
                    onSettingsClick = {},
                    onAddonsClick = {},
                    viewModel = HomeViewModel(ViewModelProvider.Factory { HomeViewModel(ApplicationProvider.getApplicationContext()) })
                )
            }
            composeRule.waitForIdle()
        }

        // Details screen startup
        measureStartup("DetailsScreen") {
            composeRule.setContent {
                DetailsScreen(
                    mediaId = "test",
                    onBackClick = {},
                    onPlayClick = { _, _ -> },
                    onTrailerClick = {},
                    onRelatedClick = {},
                    viewModel = DetailsViewModel(ViewModelProvider.Factory { DetailsViewModel(ApplicationProvider.getApplicationContext()) })
                )
            }
            composeRule.waitForIdle()
        }

        // Search screen startup
        measureStartup("SearchScreen") {
            composeRule.setContent {
                SearchScreen(onBackClick = {}, onResultClick = {})
            }
            composeRule.waitForIdle()
        }

        // Player screen startup
        measureStartup("PlayerScreen") {
            composeRule.setContent {
                PlayerScreen(
                    viewModel = PlayerViewModel(ViewModelProvider.Factory { PlayerViewModel(ApplicationProvider.getApplicationContext()) }),
                    onBackPressed = {}
                )
            }
            composeRule.waitForIdle()
        }

        // Settings screen startup
        measureStartup("SettingsScreen") {
            composeRule.setContent {
                SettingsScreen(onBackClick = {}, viewModel = SettingsViewModel(ViewModelProvider.Factory { SettingsViewModel(ApplicationProvider.getApplicationContext()) }))
            }
            composeRule.waitForIdle()
        }
    }

    private fun measureStartup(name: String, block: () -> Unit) {
        val rule = BenchmarkRule()
        rule.measureRepeated(packageName = "com.kurostream.benchmark", metrics = androidx.benchmark.BenchmarkState.metrics, iterations = 10) {
            block()
        }
    }
}