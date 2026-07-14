// This file is part of KuroStream.
//
// KuroStream is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// KuroStream is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with KuroStream.  If not, see <https://www.gnu.org/licenses/>.

package com.kurostream.domain.usecase.favorite

import com.kurostream.core.common.result.Result
import com.kurostream.domain.model.Favorite
import com.kurostream.domain.repository.MediaRepository
import kotlinx.coroutines.flow.Flow

class GetFavoritesUseCase(
    private val repository: MediaRepository
) {
    operator fun invoke(profileId: String): Flow<List<Favorite>> {
        return repository.observeFavorites(profileId)
    }
}

class IsFavoriteUseCase(
    private val repository: MediaRepository
) {
    suspend operator fun invoke(mediaItemId: String, profileId: String): Boolean {
        return repository.isFavorite(mediaItemId, profileId)
    }
}

class AddFavoriteUseCase(
    private val repository: MediaRepository
) {
    suspend operator fun invoke(favorite: Favorite): Result<Unit> {
        return try {
            repository.addFavorite(favorite)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.error(e)
        }
    }
}

class RemoveFavoriteUseCase(
    private val repository: MediaRepository
) {
    suspend operator fun invoke(mediaItemId: String, profileId: String): Result<Unit> {
        return try {
            repository.removeFavorite(mediaItemId, profileId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.error(e)
        }
    }
}

class ToggleFavoriteUseCase(
    private val isFavoriteUseCase: IsFavoriteUseCase,
    private val addFavoriteUseCase: AddFavoriteUseCase,
    private val removeFavoriteUseCase: RemoveFavoriteUseCase
) {
    suspend operator fun invoke(mediaItemId: String, profileId: String): Result<Boolean> {
        return try {
            val isFav = isFavoriteUseCase(mediaItemId, profileId)
            if (isFav) {
                removeFavoriteUseCase(mediaItemId, profileId)
                Result.success(false)
            } else {
                val newFavorite = Favorite(
                    id = "${profileId}_$mediaItemId",
                    mediaItemId = mediaItemId,
                    profileId = profileId
                )
                addFavoriteUseCase(newFavorite)
                Result.success(true)
            }
        } catch (e: Exception) {
            Result.error(e)
        }
    }
}