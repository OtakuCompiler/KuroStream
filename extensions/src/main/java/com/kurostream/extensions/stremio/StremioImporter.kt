package com.kurostream.extensions.stremio

import com.kurostream.domain.extension.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StremioImporter @Inject constructor(
    private val adapter: StremioAdapter,
) {

    suspend fun importFromUrl(addonUrl: String): Result<UnifiedExtension> {
        return try {
            val manifestUrl = if (addonUrl.endsWith("/manifest.json")) addonUrl else "$addonUrl/manifest.json"
            val result = adapter.fetchManifest(manifestUrl)
            result.map { manifest -> adapter.toUnifiedExtension(manifest, manifestUrl) }
        } catch (e: Exception) {
            Timber.e(e, "Failed to import Stremio addon: $addonUrl")
            Result.failure(e)
        }
    }

    suspend fun importFromManifest(manifestJson: String): Result<UnifiedExtension> {
        return try {
            val json = Json { ignoreUnknownKeys = true; isLenient = true }
            val manifest = json.decodeFromString<StremioManifest>(manifestJson)
            Result.success(adapter.toUnifiedExtension(manifest, "imported://${manifest.id}"))
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse Stremio manifest")
            Result.failure(e)
        }
    }
}
