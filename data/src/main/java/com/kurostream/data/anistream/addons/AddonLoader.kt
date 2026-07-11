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

package com.kurostream.data.anistream.addons

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AddonLoader @Inject constructor() {

    private val loadedAddons = mutableMapOf<String, LoadedAddon>()

    fun loadAddon(addon: Addon) {
        // Load extension code dynamically
        loadedAddons[addon.id] = LoadedAddon(addon.id, addon.sourceUrl)
    }

    fun unloadAddon(addonId: String) {
        loadedAddons.remove(addonId)
    }

    fun getLoadedAddon(addonId: String): LoadedAddon? = loadedAddons[addonId]

    fun getAllLoaded(): List<LoadedAddon> = loadedAddons.values.toList()
}

data class LoadedAddon(
    val id: String,
    val sourceUrl: String
)
