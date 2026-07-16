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

package com.kurostream.domain.usecase.watchhistory

import com.kurostream.core.common.result.Result
import com.kurostream.domain.model.WatchHistory
import com.kurostream.domain.repository.MediaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class GetWatchHistoryUseCase(
    private val repository: MediaRepository
) {
    operator fun invoke(profileId: String): Flow<List<WatchHistory>> {
        return repository.observeWatchHistory(profileId)
    }
}

class GetWatchHistoryByMediaUseCase(
    private val repository: MediaRepository
) {
    suspend operator fun invoke(mediaItemId: String, profileId: String): WatchHistory? {
        return repository.getWatchHistory(mediaItemId, profileId)
    }
}

class SaveWatchHistoryUseCase(
    private val repository: MediaRepository
) {
    suspend operator fun invoke(history: WatchHistory): Result<Unit> {
        return try {
            repository.saveWatchHistory(history)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.error(e)
        }
    }
}

class DeleteWatchHistoryUseCase(
    private val repository: MediaRepository
) {
    suspend operator fun invoke(mediaItemId: String, profileId: String): Result<Unit> {
        return try {
            repository.deleteWatchHistory(mediaItemId, profileId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.error(e)
        }
    }
}

class ClearWatchHistoryUseCase(
    private val repository: MediaRepository
) {
    suspend operator fun invoke(profileId: String): Result<Unit> {
        return try {
            val history = repository.observeWatchHistory(profileId).first()
            history.forEach { repository.deleteWatchHistory(it.mediaItemId, profileId) }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.error(e)
        }
    }
}