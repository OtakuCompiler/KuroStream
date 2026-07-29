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

package com.kurostream.app.ui.screens.details

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.viewmodel.ViewModelProvider
import androidx.test.core.app.ApplicationProvider
import com.kurostream.app.ui.screens.details.DetailsScreen
import com.kurostream.app.ui.screens.details.DetailsViewModel
import org.junit.Rule
import org.junit.Test

class DetailsScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun detailsScreen_displaysBackButton() {
        composeRule.setContent {
            DetailsScreen(
                mediaId = "test123",
                onBackClick = {},
                onPlayClick = { _, _ -> },
                onTrailerClick = {},
                onRelatedClick = {},
                viewModel = DetailsViewModel(ViewModelProvider.Factory { DetailsViewModel(ApplicationProvider.getApplicationContext()) })
            )
        }

        composeRule.onNodeWithContentDescription("Back").assertExists()
    }

    @Test
    fun detailsScreen_displaysPlayButton() {
        composeRule.setContent {
            DetailsScreen(
                mediaId = "test123",
                onBackClick = {},
                onPlayClick = { _, _ -> },
                onTrailerClick = {},
                onRelatedClick = {},
                viewModel = DetailsViewModel(ViewModelProvider.Factory { DetailsViewModel(ApplicationProvider.getApplicationContext()) })
            )
        }

        composeRule.onNodeWithText("Play").assertExists()
    }

    @Test
    fun detailsScreen_displaysTrailerButton() {
        composeRule.setContent {
            DetailsScreen(
                mediaId = "test123",
                onBackClick = {},
                onPlayClick = { _, _ -> },
                onTrailerClick = {},
                onRelatedClick = {},
                viewModel = DetailsViewModel(ViewModelProvider.Factory { DetailsViewModel(ApplicationProvider.getApplicationContext()) })
            )
        }

        composeRule.onNodeWithText("Trailer").assertExists()
    }

    @Test
    fun detailsScreen_displaysFavoriteButton() {
        composeRule.setContent {
            DetailsScreen(
                mediaId = "test123",
                onBackClick = {},
                onPlayClick = { _, _ -> },
                onTrailerClick = {},
                onRelatedClick = {},
                viewModel = DetailsViewModel(ViewModelProvider.Factory { DetailsViewModel(ApplicationProvider.getApplicationContext()) })
            )
        }

        composeRule.onNodeWithContentDescription("Add to favorites").assertExists()
    }
}