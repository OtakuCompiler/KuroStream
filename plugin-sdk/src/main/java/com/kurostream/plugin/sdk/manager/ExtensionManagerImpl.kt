package com.kurostream.plugin.sdk.manager

import com.kurostream.core.common.result.Result
import com.kurostream.domain.entity.ExtensionCapability
import com.kurostream.domain.entity.ExtensionInfo
import com.kurostream.domain.entity.SemanticVersion
import com.kurostream.plugin.sdk.api.ExtensionApi
import com.kurostream.plugin.sdk.api.ExtensionConfig
import com.kurostream.plugin.sdk.demo.MockAnimeCatalog
import com.kurostream.plugin.sdk.sandbox.ExtensionSandbox
import com.kurostream.plugin.sdk.sandbox.SandboxPolicy
import com.kurostream.plugin.sdk.security.SignatureVerifier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExtensionManagerImpl @Inject constructor(
    private val signatureVerifier: SignatureVerifier,
    private val extensionConfig: ExtensionConfig
) : ExtensionManager {

    private val lock = Mutex()
    private val extensions = mutableMapOf<String, ExtensionEntry>()
    private val _allExtensions = MutableStateFlow<List<ExtensionInfo>>(emptyList())

    init {
        val demo = MockAnimeCatalog()
        val sandbox = ExtensionSandbox(demo, SandboxPolicy())
        lock.withLock {
            extensions[demo.extensionId] = ExtensionEntry(demo.info.copy(isInstalled = true, isEnabled = true), sandbox)
        }
        emitState()
    }

    override fun observeAllExtensions(): StateFlow<List<ExtensionInfo>> = _allExtensions.asStateFlow()
    override fun observeEnabledExtensions() = _allExtensions.map { it.filter { e -> e.isEnabled } }
    override fun getExtensionApi(extensionId: String): ExtensionApi? = lock.withLock {
        extensions[extensionId]?.takeIf { it.info.isEnabled }?.sandbox
    }
    override fun getEnabledApis(): List<ExtensionApi> = lock.withLock {
        extensions.values.filter { it.info.isEnabled }.map { it.sandbox }
    }

    override suspend fun install(path: String): Result<ExtensionInfo> = lock.withLock {
        runCatching {
            val info = ExtensionInfo(
                id = "stub_${System.currentTimeMillis()}", name = "Stub Extension", author = "Unknown",
                version = SemanticVersion(1, 0, 0), packageName = "com.stub",
                capabilities = setOf(ExtensionCapability.CATALOG_BROWSE)
            )
            val sandbox = ExtensionSandbox(StubExtensionApi(info), SandboxPolicy())
            sandbox.onCreate(extensionConfig)
            extensions[info.id] = ExtensionEntry(info.copy(isInstalled = true), sandbox)
            emitState()
            Result.Success(info)
        }.getOrElse { Result.Error(it) }
    }

    override suspend fun uninstall(extensionId: String): Result<Unit> = lock.withLock {
        runCatching { extensions.remove(extensionId)?.sandbox?.onDestroy(); emitState(); Result.Success(Unit) }.getOrElse { Result.Error(it) }
    }

    override suspend fun enable(extensionId: String): Result<Unit> = lock.withLock {
        runCatching {
            val entry = extensions[extensionId] ?: throw IllegalArgumentException("Extension not found: $extensionId")
            entry.sandbox.onEnable()
            extensions[extensionId] = entry.copy(info = entry.info.copy(isEnabled = true))
            emitState(); Result.Success(Unit)
        }.getOrElse { Result.Error(it) }
    }

    override suspend fun disable(extensionId: String): Result<Unit> = lock.withLock {
        runCatching {
            val entry = extensions[extensionId] ?: throw IllegalArgumentException("Extension not found: $extensionId")
            entry.sandbox.onDisable()
            extensions[extensionId] = entry.copy(info = entry.info.copy(isEnabled = false))
            emitState(); Result.Success(Unit)
        }.getOrElse { Result.Error(it) }
    }

    override suspend fun refresh(): Result<Unit> = lock.withLock {
        runCatching { emitState(); Result.Success(Unit) }.getOrElse { Result.Error(it) }
    }

    private fun emitState() { _allExtensions.value = lock.withLock { extensions.values.map { it.info } } }
    private data class ExtensionEntry(val info: ExtensionInfo, val sandbox: ExtensionSandbox)

    private class StubExtensionApi(override val info: ExtensionInfo) : ExtensionApi {
        override val extensionId: String = info.id
        override suspend fun onCreate(config: ExtensionConfig) {}
        override suspend fun onEnable() {}
        override suspend fun onDisable() {}
        override suspend fun onDestroy() {}
        override fun getCapabilities(): Set<ExtensionCapability> = info.capabilities
        override suspend fun getHomeRows() = Result.Success(emptyList<com.kurostream.domain.entity.HomeRow>())
        override suspend fun search(query: String, page: Int, limit: Int) = Result.Success(emptyList<com.kurostream.domain.entity.MediaItem>())
        override suspend fun getAnimeDetails(mediaId: String) = Result.Error(NotImplementedError())
        override suspend fun getVideoSources(episodeId: String) = Result.Error(NotImplementedError())
        override suspend fun getSubtitleCandidates(episodeId: String) = Result.Error(NotImplementedError())
        override suspend fun reportProgress(mediaId: String, episodeNumber: Int, progressPercent: Float) = Result.Success(Unit)
        override suspend fun syncWatchlist() = Result.Success(emptyList<com.kurostream.domain.entity.MediaItem>())
    }
}