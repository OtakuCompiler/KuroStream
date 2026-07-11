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

package com.kurostream.app.ui.screens.player

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.viewmodel.ViewModelProvider
import androidx.test.core.app.ApplicationProvider
import com.kurostream.app.player.PlayerScreen
import com.kurostream.app.player.PlayerViewModel
import org.junit.Rule
import org.junit.Test

class PlayerScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun playerScreen_displaysPlayPauseButton() {
        composeRule.setContent {
            PlayerScreen(
                viewModel = PlayerViewModel(ViewModelProvider.Factory { PlayerViewModel(ApplicationProvider.getApplicationContext()) }),
                onBackPressed = {}
            )
        }

        composeRule.onNodeWithContentDescription("Pause").assertExists()
    }

    @Test
    fun playerScreen_displaysSeekBackwardButton() {
        composeRule.setContent {
            PlayerScreen(
                viewModel = PlayerViewModel(ViewModelProvider.Factory { PlayerViewModel(ApplicationProvider.getApplicationContext()) }),
                onBackPressed = {}
            )
        }

        composeRule.onNodeWithContentDescription("Back 10s").assertExists()
    }

    @Test
    fun playerScreen_displaysSeekForwardButton() {
        composeRule.setContent {
            PlayerScreen(
                viewModel = PlayerViewModel(ViewModelProvider.Factory { PlayerViewModel(ApplicationProvider.getApplicationContext()) }),
                onBackPressed = {}
            )
        }

        composeRule.onNodeWithContentDescription("Forward 10s").assertExists()
    }

    @Test
    fun playerScreen_displaysNextEpisodeButton() {
        composeRule.setContent {
            PlayerScreen(
                viewModel = PlayerViewModel(ViewModelProvider.Factory { PlayerViewModel(ApplicationProvider.getApplicationContext()) }),
                onBackPressed = {}
            )
        }

        composeRule.onNodeWithContentDescription("Next Episode").assertExists()
    }

    @Test
    fun playerScreen_displaysSettingsButton() {
        composeRule.setContent {
            PlayerScreen(
                viewModel = PlayerViewModel(ViewModelProvider.Factory { PlayerViewModel(ApplicationProvider.getApplicationContext()) }),
                onBackPressed = {}
            )
        }

        composeRule.onNodeWithContentDescription("Settings").assertExists()
    }

    @Test
    fun playerScreen_displaysBackButton() {
        composeRule.setContent {
            PlayerScreen(
                viewModel = PlayerViewModel(ViewModelProvider.Factory { PlayerViewModel(ApplicationProvider.getApplicationContext()) }),
                onBackPressed = {}
            )
        }

        composeRule.onNodeWithContentDescription("Back").assertExists()
    }
}