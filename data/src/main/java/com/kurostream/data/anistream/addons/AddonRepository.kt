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

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AddonRepository @Inject constructor(
    private val addonDao: AddonDao,
    private val addonLoader: AddonLoader
) {

    private val _installedAddons = MutableStateFlow<List<Addon>>(emptyList())
    val installedAddons: Flow<List<Addon>> = _installedAddons.asStateFlow()

    init {
        refreshAddons()
    }

    fun refreshAddons() {
        // Load from database and file system
    }

    fun getInstalledAddons(): Flow<List<Addon>> = _installedAddons

    suspend fun installAddon(addon: Addon): Result<Addon> {
        return try {
            addonDao.insert(addon)
            addonLoader.loadAddon(addon)
            refreshAddons()
            Result.success(addon)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uninstallAddon(addonId: String) {
        addonDao.deleteById(addonId)
        addonLoader.unloadAddon(addonId)
        refreshAddons()
    }

    suspend fun toggleEnabled(addonId: String) {
        val addon = addonDao.getById(addonId) ?: return
        val updated = addon.copy(isEnabled = !addon.isEnabled)
        addonDao.update(updated)
        if (updated.isEnabled) {
            addonLoader.loadAddon(updated)
        } else {
            addonLoader.unloadAddon(addonId)
        }
        refreshAddons()
    }

    suspend fun getAddonById(id: String): Addon? = addonDao.getById(id)
}
