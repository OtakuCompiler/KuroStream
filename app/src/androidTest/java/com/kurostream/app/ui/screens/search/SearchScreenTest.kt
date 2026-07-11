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

package com.kurostream.app.ui.screens.search

import androidx.compose.ui.test.junit4.createComposeRule
import com.kurostream.app.ui.screens.search.SearchScreen
import org.junit.Rule
import org.junit.Test

class SearchScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun searchScreen_displaysSearchTitle() {
        composeRule.setContent {
            SearchScreen(onBackClick = {}, onResultClick = {})
        }

        composeRule.onNodeWithText("Search").assertExists()
    }

    @Test
    fun searchScreen_displaysQueryInput() {
        composeRule.setContent {
            SearchScreen(onBackClick = {}, onResultClick = {})
        }

        composeRule.onNodeWithText("Query: ").assertExists()
    }
}