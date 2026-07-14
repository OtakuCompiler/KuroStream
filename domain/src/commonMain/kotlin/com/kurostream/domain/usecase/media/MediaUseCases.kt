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

package com.kurostream.domain.usecase.media

import com.kurostream.core.common.result.Result
import com.kurostream.domain.entity.MediaItem
import com.kurostream.domain.model.MediaCategory
import com.kurostream.domain.repository.MediaRepository
import kotlinx.coroutines.flow.Flow

class GetMediaByCategoryUseCase(
    private val repository: MediaRepository
) {
    operator fun invoke(category: MediaCategory): Flow<List<MediaItem>> {
        return repository.observeMediaByCategory(category)
    }
}

class GetMediaByIdUseCase(
    private val repository: MediaRepository
) {
    suspend operator fun invoke(id: String): MediaItem? {
        return repository.getMediaById(id)
    }
}

class SearchLocalUseCase(
    private val repository: MediaRepository
) {
    suspend operator fun invoke(query: String): List<MediaItem> {
        return repository.searchLocal(query)
    }
}

class SearchRemoteUseCase(
    private val repository: MediaRepository
) {
    suspend operator fun invoke(query: String, source: String? = null): Result<List<MediaItem>> {
        return try {
            val results = repository.searchRemote(query, source)
            Result.success(results)
        } catch (e: Exception) {
            Result.error(e)
        }
    }
}

class GetTrendingUseCase(
    private val repository: MediaRepository
) {
    suspend operator fun invoke(source: String? = null): Result<List<MediaItem>> {
        return try {
            val results = repository.getTrending(source)
            Result.success(results)
        } catch (e: Exception) {
            Result.error(e)
        }
    }
}

class GetRemoteDetailsUseCase(
    private val repository: MediaRepository
) {
    suspend operator fun invoke(mediaId: String, source: String): Result<MediaItem> {
        return try {
            val result = repository.getRemoteDetails(mediaId, source)
            if (result != null) Result.success(result)
            else Result.error(RuntimeException("Media not found"))
        } catch (e: Exception) {
            Result.error(e)
        }
    }
}

class SaveMediaItemUseCase(
    private val repository: MediaRepository
) {
    suspend operator fun invoke(item: MediaItem): Result<Unit> {
        return try {
            repository.saveMediaItem(item)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.error(e)
        }
    }
}

class DeleteMediaItemUseCase(
    private val repository: MediaRepository
) {
    suspend operator fun invoke(id: String): Result<Unit> {
        return try {
            repository.deleteMediaItem(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.error(e)
        }
    }
}