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

import android.os.Bundle
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.kurostream.launcher.data.model.PlayerState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StateRestorationManager @Inject constructor(
    private val gson: Gson
) {
    companion object {
        private const val KEY_NAV_BACKSTACK = "nav_backstack_state"
        internal const val KEY_PLAYER_STATE = "player_state"
        private const val KEY_SELECTED_TAB = "selected_tab_index"
        private const val KEY_SCROLL_POSITIONS = "scroll_positions"
        private const val KEY_SEARCH_QUERY = "search_query"
        private const val KEY_FILTER_STATE = "filter_state"
    }

    private val _isRestoring = MutableStateFlow(false)
    val isRestoring: StateFlow<Boolean> = _isRestoring

    /**
     * Save navigation backstack state before process death
     */
    fun saveNavigationState(savedStateHandle: SavedStateHandle, navController: NavController) {
        try {
            val backStackEntries = navController.backQueue
                .filter { it.destination.id != navController.graph.startDestinationId }
                .map { entry ->
                    NavEntryState(
                        destinationId = entry.destination.id,
                        arguments = entry.arguments?.toBundleMap() ?: emptyMap()
                    )
                }

            savedStateHandle[KEY_NAV_BACKSTACK] = gson.toJson(backStackEntries)
        } catch (e: Exception) {
            // Silently fail - state restoration is best-effort
        }
    }

    /**
     * Restore navigation backstack after process recreation
     */
    fun restoreNavigationState(savedStateHandle: SavedStateHandle): List<NavEntryState>? {
        val json = savedStateHandle.get<String>(KEY_NAV_BACKSTACK) ?: return null
        return try {
            val type = object : TypeToken<List<NavEntryState>>() {}.type
            gson.fromJson<List<NavEntryState>>(json, type)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Save player state for restoration
     */
    fun savePlayerState(savedStateHandle: SavedStateHandle, playerState: PlayerState) {
        savedStateHandle[KEY_PLAYER_STATE] = gson.toJson(playerState)
    }

    /**
     * Restore player state
     */
    fun restorePlayerState(savedStateHandle: SavedStateHandle): PlayerState? {
        val json = savedStateHandle.get<String>(KEY_PLAYER_STATE) ?: return null
        return try {
            gson.fromJson(json, PlayerState::class.java)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Save selected tab index
     */
    fun saveSelectedTab(savedStateHandle: SavedStateHandle, tabIndex: Int) {
        savedStateHandle[KEY_SELECTED_TAB] = tabIndex
    }

    fun restoreSelectedTab(savedStateHandle: SavedStateHandle, defaultTab: Int = 0): Int {
        return savedStateHandle.get<Int>(KEY_SELECTED_TAB) ?: defaultTab
    }

    /**
     * Save scroll positions for RecyclerViews
     */
    fun saveScrollPosition(savedStateHandle: SavedStateHandle, key: String, position: Int) {
        val positions = getScrollPositions(savedStateHandle).toMutableMap()
        positions[key] = position
        savedStateHandle[KEY_SCROLL_POSITIONS] = gson.toJson(positions)
    }

    fun restoreScrollPosition(savedStateHandle: SavedStateHandle, key: String, defaultPosition: Int = 0): Int {
        return getScrollPositions(savedStateHandle)[key] ?: defaultPosition
    }

    private fun getScrollPositions(savedStateHandle: SavedStateHandle): Map<String, Int> {
        val json = savedStateHandle.get<String>(KEY_SCROLL_POSITIONS) ?: return emptyMap()
        return try {
            val type = object : TypeToken<Map<String, Int>>() {}.type
            gson.fromJson<Map<String, Int>>(json, type) ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }

    /**
     * Save search query
     */
    fun saveSearchQuery(savedStateHandle: SavedStateHandle, query: String) {
        savedStateHandle[KEY_SEARCH_QUERY] = query
    }

    fun restoreSearchQuery(savedStateHandle: SavedStateHandle): String? {
        return savedStateHandle.get<String>(KEY_SEARCH_QUERY)
    }

    /**
     * Save filter state
     */
    fun <T> saveFilterState(savedStateHandle: SavedStateHandle, filterState: T) {
        savedStateHandle[KEY_FILTER_STATE] = gson.toJson(filterState)
    }

    inline fun <reified T> restoreFilterState(savedStateHandle: SavedStateHandle): T? {
        val json = savedStateHandle.get<String>(KEY_FILTER_STATE) ?: return null
        return try {
            gson.fromJson(json, T::class.java)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Clear all saved state
     */
    fun clearSavedState(savedStateHandle: SavedStateHandle) {
        savedStateHandle.keys().forEach { key ->
            savedStateHandle.remove<Any>(key)
        }
    }

    private fun Bundle.toBundleMap(): Map<String, Any?> {
        val map = mutableMapOf<String, Any?>()
        keySet().forEach { key ->
            map[key] = get(key)
        }
        return map
    }
}

data class NavEntryState(
    val destinationId: Int,
    val arguments: Map<String, Any?>
)

data class PlayerState(
    val mediaId: String,
    val title: String,
    val currentPositionMs: Long,
    val durationMs: Long,
    val isPlaying: Boolean,
    val playbackSpeed: Float,
    val audioTrackIndex: Int,
    val subtitleTrackIndex: Int,
    val volume: Float,
    val isMuted: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)
