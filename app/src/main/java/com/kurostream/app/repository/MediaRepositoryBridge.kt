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
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

private fun com.kurostream.domain.entity.MediaItem.toAppModel(): MediaItem = MediaItem(
    id = id,
    title = title,
    description = description ?: "",
    posterUrl = posterUrl ?: "",
    backdropUrl = backdropUrl ?: "",
    genre = genre,
    rating = rating ?: 0f,
    year = year ?: 0,
    duration = duration ?: 0,
    episodes = emptyList(),
    source = source,
    isFavorite = isFavorite,
    watchProgress = 0L,
)

@Singleton
class MediaRepositoryBridge @Inject constructor(
    private val domainRepo: com.kurostream.domain.repository.MediaRepository,
) : TvRepositories.MediaRepository {

    private val _isRefreshing = MutableStateFlow(false)
    private val _refreshError = MutableStateFlow<String?>(null)

    override val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()
    override val refreshError: StateFlow<String?> = _refreshError.asStateFlow()

    suspend fun refreshTrending() {
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
                    val e = result.data as com.kurostream.domain.model.EpisodeInfo
                    kotlin.Result.success(Episode(id = episodeId ?: mediaId, number = e.episodeNumber ?: 0, title = e.episodeTitle ?: ""))
                }
                else -> kotlin.Result.failure(Exception("Next episode not found"))
            }
        } catch (e: Exception) {
            kotlin.Result.failure(e)
        }
    }
}
