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
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.waitForIdle
import androidx.lifecycle.viewmodel.ViewModelProvider
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kurostream.app.player.PlayerActivity
import com.kurostream.app.player.PlayerScreen
import com.kurostream.app.player.PlayerViewModel
import com.kurostream.app.ui.screens.home.HomeScreen
import com.kurostream.app.ui.screens.home.HomeViewModel
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StartupBenchmark {

    @get:Rule
    val benchmarkRule = BenchmarkRule()

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun coldStartup_homeScreen() {
        benchmarkRule.measureRepeated {
            val viewModel = HomeViewModel(ViewModelProvider.Factory { HomeViewModel(ApplicationProvider.getApplicationContext()) })

            composeRule.setContent {
                HomeScreen(
                    onMediaClick = {},
                    onSearchClick = {},
                    onDownloadsClick = {},
                    onSettingsClick = {},
                    onAddonsClick = {},
                    viewModel = viewModel
                )
            }
            composeRule.waitForIdle()
        }
    }

    @Test
    fun coldStartup_detailsScreen() {
        benchmarkRule.measureRepeated {
            composeRule.setContent {
                com.kurostream.app.ui.screens.details.DetailsScreen(
                    mediaId = "test",
                    onBackClick = {},
                    onPlayClick = { _, _ -> },
                    onTrailerClick = {},
                    onRelatedClick = {},
                    viewModel = com.kurostream.app.ui.screens.details.DetailsViewModel(ViewModelProvider.Factory { com.kurostream.app.ui.screens.details.DetailsViewModel(ApplicationProvider.getApplicationContext()) })
                )
            }
            composeRule.waitForIdle()
        }
    }

    @Test
    fun coldStartup_searchScreen() {
        benchmarkRule.measureRepeated {
            composeRule.setContent {
                com.kurostream.app.ui.screens.search.SearchScreen(
                    onBackClick = {},
                    onResultClick = {}
                )
            }
            composeRule.waitForIdle()
        }
    }

    @Test
    fun coldStartup_settingsScreen() {
        benchmarkRule.measureRepeated {
            composeRule.setContent {
                com.kurostream.app.ui.screens.settings.SettingsScreen(onBackClick = {})
            }
            composeRule.waitForIdle()
        }
    }

    @Test
    fun coldStartup_playerScreen() {
        benchmarkRule.measureRepeated {
            composeRule.setContent {
                PlayerScreen(
                    viewModel = PlayerViewModel(ViewModelProvider.Factory { PlayerViewModel(ApplicationProvider.getApplicationContext()) }),
                    onBackPressed = {}
                )
            }
            composeRule.waitForIdle()
        }
    }
}