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

package com.kurostream.launcher.state

/**
 * Centralized keys for SavedStateHandle to avoid string duplication
 */
object SavedStateKeys {
    const val NAV_BACKSTACK = "nav_backstack_state"
    const val PLAYER_STATE = "player_state"
    const val SELECTED_TAB = "selected_tab_index"
    const val SCROLL_POSITIONS = "scroll_positions"
    const val SEARCH_QUERY = "search_query"
    const val FILTER_STATE = "filter_state"
    const val CURRENT_MEDIA_ID = "current_media_id"
    const val PLAYLIST_STATE = "playlist_state"
    const val UI_MODE = "ui_mode"
    const val SORT_OPTION = "sort_option"
    const val VIEW_TYPE = "view_type"
}
