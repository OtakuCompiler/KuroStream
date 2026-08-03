package com.kurostream.app.repository

import com.kurostream.app.model.MediaItem
import com.kurostream.domain.entity.MediaItem as DomainMediaItem
import com.kurostream.domain.model.Favorite
import com.kurostream.domain.repository.MediaRepository
import com.kurostream.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FavoritesRepositoryBridge @Inject constructor(
    private val mediaRepository: MediaRepository,
    private val profileRepository: ProfileRepository
) : TvRepositories.FavoritesRepository {

    private fun DomainMediaItem.toAppModel(): MediaItem = MediaItem(
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

    override fun getFavorites(): Flow<List<MediaItem>> {
        return profileRepository.observeActiveProfile().flatMapLatest { profile ->
            if (profile == null) {
                flowOf(emptyList())
            } else {
                mediaRepository.observeFavorites(profile.id).flatMapLatest { favorites ->
                    kotlinx.coroutines.flow.flow {
                        val items = favorites.mapNotNull { fav ->
                            mediaRepository.getMediaById(fav.mediaItemId)?.toAppModel()
                        }
                        emit(items)
                    }
                }
            }
        }
    }

    override suspend fun addFavorite(item: MediaItem) {
        try {
            val profile = profileRepository.getActiveProfile()
            val profileId = profile?.id ?: "default"
            val favorite = Favorite(
                id = "${profileId}_${item.id}",
                mediaItemId = item.id,
                profileId = profileId,
                category = "general"
            )
            mediaRepository.addFavorite(favorite)
        } catch (e: Exception) {
            Timber.e(e, "Failed to add favorite ${item.id}")
        }
    }

    override suspend fun removeFavorite(itemId: String) {
        try {
            val profile = profileRepository.getActiveProfile()
            val profileId = profile?.id ?: "default"
            mediaRepository.removeFavorite(itemId, profileId)
        } catch (e: Exception) {
            Timber.e(e, "Failed to remove favorite $itemId")
        }
    }
}
