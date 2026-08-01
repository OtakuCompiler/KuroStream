package com.kurostream.extensions.kodi

import com.kurostream.domain.extension.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KodiImporter @Inject constructor(
    private val adapter: KodiAdapter,
) {

    suspend fun importFromUrl(repoUrl: String): Result<List<UnifiedExtension>> {
        return try {
            val result = adapter.fetchRepository(repoUrl)
            result
        } catch (e: Exception) {
            Timber.e(e, "Failed to import Kodi repo: $repoUrl")
            Result.failure(e)
        }
    }
}
