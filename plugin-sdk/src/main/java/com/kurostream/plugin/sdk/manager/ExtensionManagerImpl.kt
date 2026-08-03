package com.kurostream.plugin.sdk.manager

import com.kurostream.domain.result.Result
import com.kurostream.domain.entity.ExtensionCapability
import com.kurostream.domain.entity.ExtensionInfo
import com.kurostream.domain.entity.SemanticVersion
import com.kurostream.plugin.sdk.api.ExtensionApi
import com.kurostream.plugin.sdk.api.ExtensionConfig
import com.kurostream.plugin.sdk.manifest.ExtensionManifestValidator
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
import android.content.Context
import android.content.pm.PackageManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExtensionManagerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val signatureVerifier: SignatureVerifier,
    private val extensionConfig: ExtensionConfig,
    private val manifestValidator: ExtensionManifestValidator
) : ExtensionManager {
    // List of trusted developer fingerprints
    private val trustedDeveloperFingerprints: Set<String> = emptySet()

    // List of allowed capabilities
    private val allowedCapabilities: Set<ExtensionCapability> = ExtensionCapability.entries.toSet()

    private val lock = Mutex()
    private val extensions = mutableMapOf<String, ExtensionEntry>()
    private val _allExtensions = MutableStateFlow<List<ExtensionInfo>>(emptyList())

    init {
        // No production extensions registered by default.
        // Users install extensions via the Addons screen (Stremio/Cloudstream).
        // The productionAnimeCatalog is available for testing if needed.
        // TEMP: runBlocking { emitState() }
    }

    override fun observeAllExtensions(): StateFlow<List<ExtensionInfo>> = _allExtensions.asStateFlow()
    override fun observeEnabledExtensions() = _allExtensions.map { it.filter { e -> e.isEnabled } }
    override suspend fun getExtensionApi(extensionId: String): ExtensionApi? = lock.withLock {
        extensions[extensionId]?.takeIf { it.info.isEnabled }?.sandbox
    }
    override suspend fun getEnabledApis(): List<ExtensionApi> = lock.withLock {
        extensions.values.filter { it.info.isEnabled }.map { it.sandbox }
    }

    override suspend fun install(path: String): Result<ExtensionInfo> = lock.withLock {
        runCatching {
            // Verify signature first
            val verificationResult = signatureVerifier.verify(path)
            if (verificationResult.isFailure) {
                return@runCatching Result.Error(Exception("Signature verification failed: ${verificationResult.exceptionOrNull()?.message}"))
            }
            
            val fingerprint = verificationResult.getOrThrow()
            
            // Check if this is a trusted developer
            val isTrustedDeveloper = trustedDeveloperFingerprints.contains(fingerprint)
            
            // In production, only allow trusted developers
            if (!extensionConfig.allowUntrustedExtensions && !isTrustedDeveloper) {
                return@runCatching Result.Error(Exception("Extension is not from a trusted developer"))
            }
            
            // Extract manifest from APK
            val manifestJson = extractManifestFromApk(path)
                ?: return@runCatching Result.Error(Exception("Failed to extract manifest from extension"))
            
            // Validate manifest
            val validationResult = manifestValidator.validateManifest(manifestJson, getFileSize(path))
            if (validationResult.isFailure) {
                return@runCatching Result.Error(
                    Exception("Manifest validation failed: ${validationResult.exceptionOrNull()?.message}")
                )
            }
            
            val info = validationResult.getOrThrow().copy(
                isTrusted = isTrustedDeveloper,
                signatureFingerprint = fingerprint
            )
            
            // Validate capabilities
            if (!allowedCapabilities.containsAll(info.capabilities)) {
                return@runCatching Result.Error(Exception("Extension declares invalid capabilities"))
            }
            
            // Create extension sandbox
            val extensionApi = createExtensionApi(info)
            val sandbox = ExtensionSandbox(extensionApi, SandboxPolicy())
            sandbox.onCreate(extensionConfig)
            
            extensions[info.id] = ExtensionEntry(info.copy(isInstalled = true), sandbox)
            emitState()
            Result.Success(info)
        }.getOrElse { Result.Error(it) }
    }
    
    private fun getFileSize(path: String): Long? {
        return try {
            java.io.File(path).length()
        } catch (e: Exception) {
            null
        }
    }
    
    private fun extractManifestFromApk(path: String): String? {
        return try {
            val packageManager = context.packageManager
            val packageInfo = packageManager.getPackageArchiveInfo(
                path,
                PackageManager.GET_ACTIVITIES or PackageManager.GET_META_DATA
            )
            
            if (packageInfo == null) {
                return null
            }
            
            val appInfo = packageInfo.applicationInfo ?: return null
            val packageName = packageInfo.packageName ?: return null
            val versionCode = packageInfo.longVersionCode
            val versionName = packageInfo.versionName ?: "1.0.0"
            val appName = appInfo.loadLabel(packageManager)?.toString() ?: packageName
            
            val metaData = appInfo.metaData ?: android.os.Bundle()
            val pluginClassName = metaData.getString("pluginClassName") ?: ""
            val apiVersion = metaData.getInt("apiVersion", 1)
            val minAppVersion = metaData.getString("minAppVersion") ?: "1.0.0"
            
            val capabilities = mutableListOf<String>()
            if (metaData.containsKey("capabilities")) {
                val caps = metaData.getString("capabilities") ?: ""
                capabilities.addAll(caps.split(",").map { it.trim() }.filter { it.isNotEmpty() })
            }
            if (capabilities.isEmpty()) {
                capabilities.addAll(listOf("CATALOG_BROWSE", "VIDEO_SOURCE"))
            }
            
            buildString {
                appendLine("{")
                appendLine("    \"id\": \"${escapeJson(packageName)}\",")
                appendLine("    \"name\": \"${escapeJson(appName)}\",")
                appendLine("    \"version\": $versionCode,")
                appendLine("    \"versionName\": \"${escapeJson(versionName)}\",")
                appendLine("    \"author\": \"${escapeJson(appName)}\",")
                appendLine("    \"pluginClassName\": \"${escapeJson(pluginClassName)}\",")
                appendLine("    \"apiVersion\": $apiVersion,")
                appendLine("    \"capabilities\": [${capabilities.joinToString { "\"${escapeJson(it)}\"" }}],")
                appendLine("    \"minAppVersion\": \"${escapeJson(minAppVersion)}\"")
                appendLine("}")
            }
        } catch (e: Exception) {
            null
        }
    }
    
    private fun escapeJson(value: String): String {
        return value.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }
    
    private fun createExtensionApi(info: ExtensionInfo): ExtensionApi {
        // This would load the extension class from the APK
        return StubExtensionApi(info)
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

    private suspend fun emitState() { _allExtensions.value = lock.withLock { extensions.values.map { it.info } } }
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
        override suspend fun getAnimeDetails(mediaId: String) = Result.Error(IllegalStateException("Extension manager requires initialization"))
        override suspend fun getVideoSources(episodeId: String) = Result.Error(IllegalStateException("Extension manager requires initialization"))
        override suspend fun getSubtitleCandidates(episodeId: String) = Result.Error(IllegalStateException("Extension manager requires initialization"))
        override suspend fun reportProgress(mediaId: String, episodeNumber: Int, progressPercent: Float) = Result.Success(Unit)
        override suspend fun syncWatchlist() = Result.Success(emptyList<com.kurostream.domain.entity.MediaItem>())
    }
}