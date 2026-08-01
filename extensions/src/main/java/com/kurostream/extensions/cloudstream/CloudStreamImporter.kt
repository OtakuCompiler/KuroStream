package com.kurostream.extensions.cloudstream

import com.kurostream.domain.extension.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CloudStreamImporter @Inject constructor(
    private val adapter: CloudStreamAdapter,
) {

    suspend fun importFromRepo(repoUrl: String): Result<List<UnifiedExtension>> {
        return try {
            val result = adapter.fetchRepository(repoUrl)
            result.map { repo -> adapter.toUnifiedExtensions(repo, repoUrl) }
        } catch (e: Exception) {
            Timber.e(e, "Failed to import CloudStream repo: $repoUrl")
            Result.failure(e)
        }
    }
}
