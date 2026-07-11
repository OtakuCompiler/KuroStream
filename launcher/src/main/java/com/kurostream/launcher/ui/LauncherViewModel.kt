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

package com.kurostream.launcher.ui

import android.app.Application
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LauncherViewModel @Inject constructor(
    application: Application,
    private val preferences: LauncherPreferences
) : AndroidViewModel(application) {

    private val _tiles = MutableStateFlow<List<LauncherTile>>(emptyList())
    val tiles: StateFlow<List<LauncherTile>> = _tiles.asStateFlow()

    private val _isDefaultLauncher = MutableStateFlow(false)
    val isDefaultLauncher: StateFlow<Boolean> = _isDefaultLauncher.asStateFlow()

    init {
        checkDefaultLauncherStatus()
    }

    fun checkDefaultLauncherStatus() {
        val pm = getApplication<Application>().packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val resolveInfo = pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        _isDefaultLauncher.value = resolveInfo?.activityInfo?.packageName == getApplication<Application>().packageName
    }

    fun loadTiles() {
        viewModelScope.launch {
            val customTiles = preferences.getCustomTiles()
            if (customTiles.isNotEmpty()) {
                _tiles.value = customTiles
            } else {
                _tiles.value = getDefaultTiles()
            }
        }
    }

    private fun getDefaultTiles(): List<LauncherTile> {
        val pm = getApplication<Application>().packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val apps = pm.queryIntentActivities(intent, 0)

        val tiles = mutableListOf<LauncherTile>()

        // Add StreamBox first
        tiles.add(
            LauncherTile(
                id = "streambox",
                title = "StreamBox",
                description = "Your media center",
                iconRes = R.drawable.ic_streambox,
                type = TileType.STREAMBOX,
                packageName = getApplication<Application>().packageName,
                order = 0
            )
        )

        // Add installed apps
        val sortedApps = apps
            .filter { it.activityInfo.packageName != getApplication<Application>().packageName }
            .sortedBy { it.loadLabel(pm).toString() }
            .take(15)

        sortedApps.forEachIndexed { index, app ->
            tiles.add(
                LauncherTile(
                    id = "app_${app.activityInfo.packageName}",
                    title = app.loadLabel(pm).toString(),
                    description = "App",
                    iconDrawable = app.loadIcon(pm),
                    type = TileType.APP,
                    packageName = app.activityInfo.packageName,
                    activityName = app.activityInfo.name,
                    order = index + 1
                )
            )
        }

        // Add Settings shortcut
        tiles.add(
            LauncherTile(
                id = "settings",
                title = "Settings",
                description = "System settings",
                iconRes = android.R.drawable.ic_menu_preferences,
                type = TileType.SETTINGS,
                order = tiles.size
            )
        )

        tiles.sortedBy { it.order }
    }

    fun reorderTiles(tiles: List<LauncherTile>) {
        viewModelScope.launch {
            preferences.saveCustomTiles(tiles)
            _tiles.value = tiles
        }
    }
}
