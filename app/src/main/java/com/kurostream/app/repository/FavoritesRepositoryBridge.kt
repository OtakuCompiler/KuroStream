package com.kurostream.app.repository

import com.kurostream.app.model.MediaItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FavoritesRepositoryBridge @Inject constructor() : TvRepositories.FavoritesRepository {

    private val favorites = mutableListOf<MediaItem>()

    override fun getFavorites(): Flow<List<MediaItem>> {
        return flow { emit(favorites.toList()) }
    }

    override suspend fun addFavorite(item: MediaItem) {
        if (favorites.none { it.id == item.id }) {
            favorites.add(item)
        }
    }

    override suspend fun removeFavorite(itemId: String) {
        favorites.removeAll { it.id == itemId }
    }
}
