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

package com.kurostream.extensions.cloudstream

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CloudstreamPluginRepository @Inject constructor(
    private val loader: CloudstreamPluginLoader,
    private val repoParser: CloudstreamRepositoryParser
) {
    private val _availablePlugins = MutableStateFlow<List<CloudstreamRepoEntry>>(emptyList())
    val availablePlugins: StateFlow<List<CloudstreamRepoEntry>> = _availablePlugins.asStateFlow()

    private val _installedPlugins = MutableStateFlow<List<CloudstreamManifest>>(emptyList())
    val installedPlugins: StateFlow<List<CloudstreamManifest>> = _installedPlugins.asStateFlow()

    suspend fun loadRepository(url: String): Result<Unit> {
        return repoParser.parseRepository(url).fold(
            onSuccess = {
                _availablePlugins.value = it
                Result.success(Unit)
            },
            onFailure = { Result.failure(it) }
        )
    }

    suspend fun installPlugin(entry: CloudstreamRepoEntry): Result<CloudstreamManifest> {
        return loader.loadPluginFromUrl(entry.url).fold(
            onSuccess = {
                refreshInstalledPlugins()
                Result.success(it)
            },
            onFailure = { Result.failure(it) }
        )
    }

    suspend fun installPluginFromFile(file: java.io.File): Result<CloudstreamManifest> {
        return loader.loadPluginFromApk(file).fold(
            onSuccess = {
                refreshInstalledPlugins()
                Result.success(it)
            },
            onFailure = { Result.failure(it) }
        )
    }

    fun uninstallPlugin(id: String) {
        loader.unloadPlugin(id)
        refreshInstalledPlugins()
    }

    private fun refreshInstalledPlugins() {
        _installedPlugins.value = loader.getLoadedPlugins().map { it.manifest }
    }
}
