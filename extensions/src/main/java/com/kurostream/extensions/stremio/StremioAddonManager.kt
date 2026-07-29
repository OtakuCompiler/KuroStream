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

package com.kurostream.extensions.stremio

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StremioAddonManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("stremio_addons", Context.MODE_PRIVATE)

    private val _installedAddons = MutableStateFlow<List<InstalledAddon>>(emptyList())
    val installedAddons: StateFlow<List<InstalledAddon>> = _installedAddons.asStateFlow()

    init {
        loadAddons()
    }

    private fun loadAddons() {
        val addonSet = prefs.getStringSet("addons", emptySet()) ?: emptySet()
        _installedAddons.value = addonSet.map { url ->
            InstalledAddon(
                url = url,
                name = prefs.getString("addon_name_$url", url) ?: url,
                isEnabled = prefs.getBoolean("addon_enabled_$url", true)
            )
        }
    }

    fun addAddon(url: String, name: String? = null) {
        val current = _installedAddons.value.toMutableList()
        if (current.none { it.url == url }) {
            current.add(InstalledAddon(url = url, name = name ?: url, isEnabled = true))
            _installedAddons.value = current
            saveAddons()
        }
    }

    fun removeAddon(url: String) {
        _installedAddons.value = _installedAddons.value.filter { it.url != url }
        saveAddons()
    }

    fun toggleAddon(url: String, enabled: Boolean) {
        _installedAddons.value = _installedAddons.value.map {
            if (it.url == url) it.copy(isEnabled = enabled) else it
        }
        saveAddons()
    }

    private fun saveAddons() {
        val urls = _installedAddons.value.map { it.url }.toSet()
        prefs.edit {
            putStringSet("addons", urls)
            _installedAddons.value.forEach { addon ->
                putString("addon_name_${addon.url}", addon.name)
                putBoolean("addon_enabled_${addon.url}", addon.isEnabled)
            }
        }
    }

    fun getEnabledAddons(): List<InstalledAddon> = _installedAddons.value.filter { it.isEnabled }

    data class InstalledAddon(
        val url: String,
        val name: String,
        val isEnabled: Boolean
    )
}
