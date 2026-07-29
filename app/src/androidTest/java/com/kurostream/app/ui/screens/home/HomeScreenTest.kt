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

package com.kurostream.app.ui.screens.home

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.viewmodel.ViewModelProvider
import androidx.test.core.app.ApplicationProvider
import org.junit.Rule
import org.junit.Test

class HomeScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun homeScreen_displaysSearchButton() {
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

        composeRule.onNodeWithText("Search").assertExists()
    }

    @Test
    fun homeScreen_displaysSettingsButton() {
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

        composeRule.onNodeWithText("Settings").assertExists()
    }

    @Test
    fun homeScreen_displaysDownloadsButton() {
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

        composeRule.onNodeWithText("Downloads").assertExists()
    }
}