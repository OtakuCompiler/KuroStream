package com.kurostream.app.repository

import com.kurostream.app.model.MediaItem
import com.kurostream.app.model.Episode
import com.kurostream.app.model.PlaybackUrl
import com.kurostream.domain.result.Result as DomainResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Maps a domain-layer [com.kurostream.domain.entity.MediaItem] to the
 * app-layer [MediaItem] used by the UI.
 *
 * This function was called in four places in this file (getMediaItems,
 * getMediaItem, search) but was never defined anywhere in the codebase —
 * every one of those call sites would fail with "unresolved reference:
 * toAppModel" at compile time. Field mapping below is a best-effort
 * pairing based on the two data classes' shapes; watchProgress and
 * episodes are not present on the domain entity and are left at their
 * defaults (populated elsewhere, e.g. HomeViewModel attaches watch
 * progress from a separate repository).
 */
private fun com.kurostream.domain.entity.MediaItem.toAppModel(): MediaItem = MediaItem(
    id = id,
    title = title,
    description = synopsis ?: "",
    posterUrl = coverImageUrl ?: posterUrl ?: "",
    backdropUrl = bannerImageUrl ?: backdropUrl ?: "",
    genre = genres,
    rating = score?.toFloat() ?: 0f,
    year = seasonYear ?: 0,
    duration = durationMinutes ?: 0,
    episodes = emptyList(),
    source = sourceExtensionId,
    isFavorite = isFavorite,
    watchProgress = 0L,
)

@Singleton
class MediaRepositoryBridge @Inject constructor(
    private val domainRepo: com.kurostream.domain.repository.MediaRepository,
) : TvRepositories.MediaRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _isRefreshing = MutableStateFlow(false)
    private val _refreshError = MutableStateFlow<String?>(null)

    override val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()
    override val refreshError: StateFlow<String?> = _refreshError.asStateFlow()

    init {
        scope.launch {
            refreshTrending()
        }
    }

    private suspend fun refreshTrending() {
        _isRefreshing.value = true
        _refreshError.value = null
        try {
            val trending = domainRepo.getTrending()
            if (trending.isNotEmpty()) {
                domainRepo.saveMediaItems(trending)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch trending")
            _refreshError.value = e.message
        } finally {
            _isRefreshing.value = false
        }
    }

    override fun getMediaItems(): Flow<List<MediaItem>> {
        return domainRepo.observeAllMediaItems()
            .map { domainList -> domainList.map { it.toAppModel() } }
            .catch { e ->
                Timber.e(e, "Error loading media items")
                _refreshError.value = e.message
                emit(emptyList())
            }
    }

    override fun getMediaItem(id: String): Flow<MediaItem?> {
        return flow {
            val domainItem = domainRepo.getMediaById(id)
            emit(domainItem?.toAppModel())
        }
    }

    override suspend fun search(query: String): List<MediaItem> {
        return try {
            val results = domainRepo.searchRemote(query)
            results.map { it.toAppModel() }
        } catch (e: Exception) {
            Timber.e(e, "Search failed")
            emptyList()
        }
    }

    override suspend fun getPlaybackUrl(mediaId: String, episodeId: String?): kotlin.Result<PlaybackUrl> {
        return try {
            val result = domainRepo.getPlaybackUrl(mediaId, episodeId)
            when (result) {
                is DomainResult.Success<*> -> {
                    val r = result.data as com.kurostream.domain.model.PlaybackUrl
                    kotlin.Result.success(PlaybackUrl(url = r.url, headers = r.headers, quality = r.quality))
                }
                else -> kotlin.Result.failure(Exception("Playback URL not found"))
            }
        } catch (e: Exception) {
            kotlin.Result.failure(e)
        }
    }

    override suspend fun getNextEpisode(mediaId: String, episodeId: String?): kotlin.Result<Episode> {
        return try {
            val result = domainRepo.getNextEpisode(mediaId, episodeId)
            when (result) {
                is DomainResult.Success<*> -> {
                    val e = result.data as com.kurostream.domain.model.Episode
                    kotlin.Result.success(Episode(id = e.id, number = e.episodeNumber, title = e.title))
                }
                else -> kotlin.Result.failure(Exception("Next episode not found"))
            }
        } catch (e: Exception) {
            kotlin.Result.failure(e)
        }
    }
}
