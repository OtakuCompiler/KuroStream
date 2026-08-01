package com.kurostream.data.repository

import com.kurostream.data.local.dao.ExtensionDao
import com.kurostream.data.local.entity.ExtensionEntity
import com.kurostream.data.local.entity.toDomain
import com.kurostream.data.local.entity.toEntity
import com.kurostream.domain.extension.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExtensionRepositoryImpl @Inject constructor(
    private val extensionDao: ExtensionDao,
    private val json: Json,
) : ExtensionRepository {

    override fun observeAll(): Flow<List<UnifiedExtension>> {
        return extensionDao.observeAll().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getExtension(id: String): UnifiedExtension? {
        return extensionDao.getById(id)?.toDomain()
    }

    override suspend fun install(extension: UnifiedExtension) {
        val entity = extension.toEntity().copy(
            isInstalled = true,
            isEnabled = true,
            installedAt = System.currentTimeMillis(),
        )
        extensionDao.insert(entity)
        Timber.d("Installed extension: ${extension.id}")
    }

    override suspend fun uninstall(id: String) {
        extensionDao.deleteById(id)
        Timber.d("Uninstalled extension: $id")
    }

    override suspend fun enable(id: String) {
        extensionDao.setEnabled(id, true)
        Timber.d("Enabled extension: $id")
    }

    override suspend fun disable(id: String) {
        extensionDao.setEnabled(id, false)
        Timber.d("Disabled extension: $id")
    }

    override suspend fun getConfig(id: String): Map<String, String> {
        val entity = extensionDao.getById(id) ?: return emptyMap()
        return try {
            val config = json.decodeFromString<Map<String, String>>(entity.configSchema)
            config
        } catch (e: Exception) {
            emptyMap()
        }
    }

    override suspend fun setConfig(id: String, values: Map<String, String>) {
        val entity = extensionDao.getById(id) ?: return
        val newConfigSchema = json.encodeToString(values)
        extensionDao.update(entity.copy(configSchema = newConfigSchema))
    }

    override suspend fun checkForUpdates(): List<UnifiedExtension> {
        return emptyList()
    }
}
