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

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NavigationStateSaver @Inject constructor(
    private val stateRestorationManager: StateRestorationManager
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    /**
     * Attach to NavController to automatically save/restore state
     */
    fun attach(navController: NavController, savedStateHandle: SavedStateHandle) {
        // Save state when destination changes
        navController.addOnDestinationChangedListener { controller, _, _ ->
            scope.launch {
                stateRestorationManager.saveNavigationState(savedStateHandle, controller)
            }
        }
    }

    /**
     * Restore navigation state after process death
     */
    fun restore(navController: NavController, savedStateHandle: SavedStateHandle) {
        val entries = stateRestorationManager.restoreNavigationState(savedStateHandle)
            ?: return

        entries.forEach { entry ->
            try {
                val args = android.os.Bundle().apply {
                    entry.arguments.forEach { (key, value) ->
                        when (value) {
                            is String -> putString(key, value)
                            is Int -> putInt(key, value)
                            is Long -> putLong(key, value)
                            is Boolean -> putBoolean(key, value)
                            is Float -> putFloat(key, value)
                        }
                    }
                }
                navController.navigate(entry.destinationId, args, NavOptions.Builder().build())
            } catch (e: Exception) {
                // Skip invalid entries
            }
        }
    }

    /**
     * Clear saved navigation state
     */
    fun clear(savedStateHandle: SavedStateHandle) {
        stateRestorationManager.clearSavedState(savedStateHandle)
    }
}
