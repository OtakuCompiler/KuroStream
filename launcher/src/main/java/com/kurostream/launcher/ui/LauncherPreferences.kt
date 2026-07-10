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

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LauncherPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("launcher_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val KEY_CUSTOM_TILES = "custom_tiles"
        private const val KEY_LAUNCHER_ENABLED = "launcher_enabled"
    }

    fun getCustomTiles(): List<LauncherTile> {
        val json = prefs.getString(KEY_CUSTOM_TILES, null) ?: return emptyList()
        val type = object : TypeToken<List<LauncherTile>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }

    fun saveCustomTiles(tiles: List<LauncherTile>) {
        prefs.edit().putString(KEY_CUSTOM_TILES, gson.toJson(tiles)).apply()
    }

    fun isLauncherEnabled(): Boolean = prefs.getBoolean(KEY_LAUNCHER_ENABLED, false)

    fun setLauncherEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_LAUNCHER_ENABLED, enabled).apply()
    }
}
