package com.kurostream.tv.data.adapter.cloudstream

import android.content.Context
import dalvik.system.DexClassLoader
import com.kurostream.tv.di.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * CloudStream Plugin Loader.
 *
 * Loads .cs3 / .jar plugin files using [DexClassLoader] and wraps each provider
 * class found inside via reflection into an [AnimeSourceProvider] instance that
 * the rest of the app can call without knowledge of the plugin's internals.
 *
 * Plugin lifecycle:
 *  1. [installPlugin]  — download from URL → save to [pluginDir]
 *  2. [loadPlugin]     — DexClassLoader → instantiate → cache providers
 *  3. [unloadPlugin]   — remove from caches
 *  4. [deletePlugin]   — unload + delete file
 *
 * Network requests use the app-wide [OkHttpClient] (10 MB cache, shared pool)
 * instead of creating an ad-hoc client.
 */
@Singleton
class CloudStreamPluginLoader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {

    companion object {
        private const val TAG = "CloudStreamPluginLoader"
        private const val PLUGIN_DIR = "cloudstream_plugins"
    }

    private val loadedPlugins = mutableMapOf<String, CloudStreamPlugin>()
    private val pluginProviders = mutableMapOf<String, List<AnimeSourceProvider>>()

    private val pluginDir: File by lazy {
        File(context.filesDir, PLUGIN_DIR).also { it.mkdirs() }
    }

    // ─── Bulk load ────────────────────────────────────────────────────────────

    /**
     * Load all .cs3 / .jar files already present in [pluginDir].
     * @return number of successfully loaded plugins.
     */
    suspend fun loadAllPlugins(): Result<Int> = withContext(ioDispatcher) {
        runCatching {
            val files = pluginDir.listFiles { f -> f.extension == "cs3" || f.extension == "jar" }
                ?: emptyArray()
            var loaded = 0
            for (file in files) {
                loadPlugin(file)
                    .onSuccess { loaded++ }
                    .onFailure { e -> Timber.tag(TAG).e(e, "Failed to load plugin: ${file.name}") }
            }
            Timber.tag(TAG).i("Loaded $loaded / ${files.size} plugins from $pluginDir")
            loaded
        }.onFailure { e ->
            Timber.tag(TAG).e(e, "loadAllPlugins() failed")
        }
    }

    // ─── Single plugin load ───────────────────────────────────────────────────

    /**
     * Load a single plugin [file] and register its providers.
     */
    suspend fun loadPlugin(file: File): Result<CloudStreamPlugin> = withContext(ioDispatcher) {
        runCatching {
            val optimizedDir = File(context.cacheDir, "plugin_dex").also { it.mkdirs() }
            val classLoader = DexClassLoader(
                file.absolutePath,
                optimizedDir.absolutePath,
                null,
                context.classLoader
            )
            val manifestClass = classLoader.loadClass("CloudStreamPlugin")
            val instance = manifestClass.getDeclaredConstructor().newInstance()
            val plugin = CloudStreamPlugin(
                id            = getStringField(instance, "id") ?: file.nameWithoutExtension,
                name          = getStringField(instance, "name") ?: file.nameWithoutExtension,
                version       = getStringField(instance, "version") ?: "1.0.0",
                description   = getStringField(instance, "description"),
                author        = getStringField(instance, "author"),
                repositoryUrl = getStringField(instance, "repositoryUrl"),
                filePath      = file.absolutePath,
                classLoader   = classLoader,
                status        = PluginStatus.LOADED
            )
            val providers = loadProviders(classLoader, plugin.id)
            pluginProviders[plugin.id] = providers
            loadedPlugins[plugin.id] = plugin
            Timber.tag(TAG).i("Loaded plugin: ${plugin.name} (${providers.size} providers)")
            plugin
        }.onFailure { e ->
            Timber.tag(TAG).e(e, "Failed to load plugin: ${file.name}")
        }
    }

    // ─── Plugin management ────────────────────────────────────────────────────

    fun unloadPlugin(pluginId: String) {
        loadedPlugins.remove(pluginId)
        pluginProviders.remove(pluginId)
    }

    fun getLoadedPlugins(): List<CloudStreamPlugin> = loadedPlugins.values.toList()
    fun getAllProviders(): List<AnimeSourceProvider> = pluginProviders.values.flatten()
    fun getProvidersFromPlugin(pluginId: String): List<AnimeSourceProvider> =
        pluginProviders[pluginId] ?: emptyList()

    // ─── Install / delete ─────────────────────────────────────────────────────

    /**
     * Download a plugin from [url] and save it as [filename] in [pluginDir].
     */
    suspend fun installPlugin(url: String, filename: String): Result<File> =
        withContext(ioDispatcher) {
            runCatching {
                val response = okHttpClient.newCall(Request.Builder().url(url).build()).execute()
                if (!response.isSuccessful) error("HTTP ${response.code} for $url")
                val file = File(pluginDir, filename)
                response.body?.byteStream()?.use { input ->
                    file.outputStream().use { output -> input.copyTo(output) }
                }
                Timber.tag(TAG).i("Plugin downloaded: $filename")
                file
            }.onFailure { e ->
                Timber.tag(TAG).e(e, "Failed to download plugin: $url")
            }
        }

    fun deletePlugin(pluginId: String): Boolean {
        val plugin = loadedPlugins[pluginId] ?: return false
        unloadPlugin(pluginId)
        return File(plugin.filePath).delete().also { deleted ->
            if (deleted) Timber.tag(TAG).i("Deleted plugin: $pluginId")
        }
    }

    // ─── Repository listing ───────────────────────────────────────────────────

    /**
     * Fetch the JSON plugin catalog from a repository URL.
     * Expects a JSON array of plugin descriptors (see [CloudStreamPluginInfo]).
     */
    suspend fun fetchAvailablePlugins(repositoryUrl: String): Result<List<CloudStreamPluginInfo>> =
        withContext(ioDispatcher) {
            runCatching {
                val response = okHttpClient
                    .newCall(Request.Builder().url(repositoryUrl).build())
                    .execute()
                if (!response.isSuccessful) error("HTTP ${response.code} for $repositoryUrl")
                val body = response.body?.string() ?: error("Empty response from $repositoryUrl")
                parsePluginList(body)
            }.onFailure { e ->
                Timber.tag(TAG).e(e, "Failed to fetch plugin repository: $repositoryUrl")
            }
        }

    // ─── Reflection helpers ───────────────────────────────────────────────────

    private fun getStringField(instance: Any, fieldName: String): String? = runCatching {
        val field = instance.javaClass.getDeclaredField(fieldName).apply { isAccessible = true }
        field.get(instance) as? String
    }.getOrNull()

    private fun loadProviders(classLoader: ClassLoader, pluginId: String): List<AnimeSourceProvider> {
        return runCatching {
            val registryClass = classLoader.loadClass("ProviderRegistry")
            val registry = registryClass.getDeclaredConstructor().newInstance()
            @Suppress("UNCHECKED_CAST")
            val providerClasses = registryClass.getMethod("getProviders")
                .invoke(registry) as? List<Class<*>> ?: return emptyList()
            providerClasses.mapNotNull { cls ->
                runCatching { createProviderWrapper(cls, pluginId) }
                    .onFailure { e -> Timber.tag(TAG).e(e, "Failed to wrap provider: ${cls.name}") }
                    .getOrNull()
            }
        }.getOrDefault(emptyList())
    }

    private fun createProviderWrapper(providerClass: Class<*>, pluginId: String): AnimeSourceProvider {
        val instance = providerClass.getDeclaredConstructor().newInstance()
        return CloudStreamProviderWrapper(
            pluginId  = pluginId,
            name      = getStringField(instance, "name") ?: providerClass.simpleName,
            language  = getStringField(instance, "lang") ?: "en",
            mainUrl   = getStringField(instance, "mainUrl") ?: "",
            providerInstance = instance,
            providerClass    = providerClass
        )
    }

    private fun parsePluginList(json: String): List<CloudStreamPluginInfo> =
        runCatching {
            val array = JSONArray(json)
            (0 until array.length()).map { i ->
                val obj = array.getJSONObject(i)
                CloudStreamPluginInfo(
                    id          = obj.getString("id"),
                    name        = obj.getString("name"),
                    version     = obj.getString("version"),
                    description = obj.optString("description").ifEmpty { null },
                    author      = obj.optString("author").ifEmpty { null },
                    downloadUrl = obj.getString("url"),
                    iconUrl     = obj.optString("iconUrl").ifEmpty { null },
                    language    = obj.optString("language").ifEmpty { "en" },
                    isAdult     = obj.optBoolean("adult", false)
                )
            }
        }.onFailure { e ->
            Timber.tag(TAG).e(e, "Failed to parse plugin list JSON")
        }.getOrDefault(emptyList())
}

// ─── Data models ──────────────────────────────────────────────────────────────

data class CloudStreamPlugin(
    val id: String,
    val name: String,
    val version: String,
    val description: String?,
    val author: String?,
    val repositoryUrl: String?,
    val filePath: String,
    val classLoader: ClassLoader,
    val status: PluginStatus
)

enum class PluginStatus { LOADED, DISABLED, ERROR }

data class CloudStreamPluginInfo(
    val id: String,
    val name: String,
    val version: String,
    val description: String?,
    val author: String?,
    val downloadUrl: String,
    val iconUrl: String?,
    val language: String,
    val isAdult: Boolean
)

// ─── Provider interface ───────────────────────────────────────────────────────

interface AnimeSourceProvider {
    val pluginId: String
    val name: String
    val language: String
    val mainUrl: String

    suspend fun search(query: String): List<AnimeSearchResult>
    suspend fun getAnimeDetails(url: String): AnimeDetails?
    suspend fun getEpisodes(animeUrl: String): List<EpisodeInfo>
    suspend fun getStreamLinks(episodeUrl: String): List<StreamLink>
}

// ─── Reflection-based wrapper ─────────────────────────────────────────────────

/**
 * Wraps a dynamically loaded CloudStream provider class via reflection so the
 * host app never depends directly on the plugin's compiled types.
 */
class CloudStreamProviderWrapper(
    override val pluginId: String,
    override val name: String,
    override val language: String,
    override val mainUrl: String,
    private val providerInstance: Any,
    private val providerClass: Class<*>
) : AnimeSourceProvider {

    override suspend fun search(query: String): List<AnimeSearchResult> = runCatching {
        @Suppress("UNCHECKED_CAST")
        val raw = providerClass.getMethod("search", String::class.java)
            .invoke(providerInstance, query) as? List<*> ?: return emptyList()
        raw.mapNotNull { convertToSearchResult(it) }
    }.getOrDefault(emptyList())

    override suspend fun getAnimeDetails(url: String): AnimeDetails? = runCatching {
        convertToAnimeDetails(
            providerClass.getMethod("load", String::class.java).invoke(providerInstance, url)
        )
    }.getOrNull()

    override suspend fun getEpisodes(animeUrl: String): List<EpisodeInfo> =
        getAnimeDetails(animeUrl)?.episodes ?: emptyList()

    override suspend fun getStreamLinks(episodeUrl: String): List<StreamLink> = runCatching {
        @Suppress("UNCHECKED_CAST")
        val raw = providerClass.getMethod("loadLinks", String::class.java)
            .invoke(providerInstance, episodeUrl) as? List<*> ?: return emptyList()
        raw.mapNotNull { convertToStreamLink(it) }
    }.getOrDefault(emptyList())

    // ─── Reflection converters ────────────────────────────────────────────────

    private fun convertToSearchResult(obj: Any?): AnimeSearchResult? {
        obj ?: return null
        return runCatching {
            AnimeSearchResult(
                name      = field(obj, "name") as? String ?: return null,
                url       = field(obj, "url")  as? String ?: return null,
                posterUrl = field(obj, "posterUrl") as? String,
                year      = field(obj, "year") as? Int,
                type      = field(obj, "type") as? String
            )
        }.getOrNull()
    }

    private fun convertToAnimeDetails(obj: Any?): AnimeDetails? {
        obj ?: return null
        return runCatching {
            @Suppress("UNCHECKED_CAST")
            val episodes = (field(obj, "episodes") as? List<*>)
                ?.mapNotNull { convertToEpisodeInfo(it) } ?: emptyList()
            AnimeDetails(
                name      = field(obj, "name") as? String ?: return null,
                url       = field(obj, "url")  as? String ?: return null,
                posterUrl = field(obj, "posterUrl") as? String,
                plot      = field(obj, "plot") as? String,
                year      = field(obj, "year") as? Int,
                rating    = field(obj, "rating") as? Float,
                tags      = (field(obj, "tags") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                episodes  = episodes
            )
        }.getOrNull()
    }

    private fun convertToEpisodeInfo(obj: Any?): EpisodeInfo? {
        obj ?: return null
        return runCatching {
            EpisodeInfo(
                name        = field(obj, "name") as? String,
                url         = field(obj, "data") as? String ?: return null,
                episode     = field(obj, "episode") as? Int,
                season      = field(obj, "season") as? Int,
                posterUrl   = field(obj, "posterUrl") as? String,
                description = field(obj, "description") as? String
            )
        }.getOrNull()
    }

    private fun convertToStreamLink(obj: Any?): StreamLink? {
        obj ?: return null
        return runCatching {
            @Suppress("UNCHECKED_CAST")
            StreamLink(
                url      = field(obj, "url")  as? String ?: return null,
                name     = field(obj, "name") as? String ?: "Unknown",
                quality  = field(obj, "quality") as? Int,
                isM3u8   = field(obj, "isM3u8") as? Boolean ?: false,
                referer  = field(obj, "referer") as? String,
                headers  = (field(obj, "headers") as? Map<*, *>)
                    ?.entries?.associate { it.key.toString() to it.value.toString() }
                    ?: emptyMap()
            )
        }.getOrNull()
    }

    /** Try the field directly, fall back to a `getXxx()` method. */
    private fun field(obj: Any, name: String): Any? = runCatching {
        obj.javaClass.getDeclaredField(name).apply { isAccessible = true }.get(obj)
    }.recoverCatching {
        val getter = "get${name.replaceFirstChar { it.uppercase() }}"
        obj.javaClass.getMethod(getter).invoke(obj)
    }.getOrNull()
}

// ─── CloudStream data models ──────────────────────────────────────────────────

data class AnimeSearchResult(
    val name: String,
    val url: String,
    val posterUrl: String?,
    val year: Int?,
    val type: String?
)

data class AnimeDetails(
    val name: String,
    val url: String,
    val posterUrl: String?,
    val plot: String?,
    val year: Int?,
    val rating: Float?,
    val tags: List<String>,
    val episodes: List<EpisodeInfo>
)

data class EpisodeInfo(
    val name: String?,
    val url: String,
    val episode: Int?,
    val season: Int?,
    val posterUrl: String?,
    val description: String?
)

data class StreamLink(
    val url: String,
    val name: String,
    val quality: Int?,
    val isM3u8: Boolean,
    val referer: String?,
    val headers: Map<String, String>
)
