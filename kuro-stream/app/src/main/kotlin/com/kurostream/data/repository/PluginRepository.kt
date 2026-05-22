package com.kurostream.data.repository

import com.kurostream.data.model.Plugin
import com.kurostream.data.source.local.PluginDao
import com.kurostream.data.source.local.PluginEntity
import com.kurostream.core.plugin.PluginType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PluginRepository @Inject constructor(
    private val pluginDao: PluginDao
) {

    fun getAllPlugins(): Flow<List<Plugin>> =
        pluginDao.getAllPlugins().map { entities -> entities.map { it.toModel() } }

    fun getEnabledPlugins(): Flow<List<Plugin>> =
        pluginDao.getEnabledPlugins().map { entities -> entities.map { it.toModel() } }

    suspend fun insertPlugin(plugin: Plugin) {
        pluginDao.insert(plugin.toEntity())
    }

    suspend fun deletePlugin(pluginId: String) {
        pluginDao.deleteById(pluginId)
    }

    suspend fun updatePluginEnabled(pluginId: String, enabled: Boolean) {
        pluginDao.setEnabled(pluginId, enabled)
    }

    private fun PluginEntity.toModel() = Plugin(
        id = id,
        name = name,
        type = PluginType.valueOf(type),
        baseUrl = baseUrl,
        manifestUrl = manifestUrl,
        version = version,
        description = description,
        isEnabled = isEnabled,
        logoUrl = logoUrl,
        supportedTypes = if (supportedTypes.isBlank()) emptyList() else supportedTypes.split(",")
    )

    private fun Plugin.toEntity() = PluginEntity(
        id = id,
        name = name,
        type = type.name,
        baseUrl = baseUrl,
        manifestUrl = manifestUrl,
        version = version,
        description = description,
        isEnabled = isEnabled,
        logoUrl = logoUrl,
        supportedTypes = supportedTypes.joinToString(",")
    )
}
