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

package com.kurostream.domain.usecase.download

import com.kurostream.core.common.result.Result
import com.kurostream.domain.model.DownloadItem
import com.kurostream.domain.model.DownloadStatus
import com.kurostream.domain.repository.MediaRepository
import kotlinx.coroutines.flow.Flow

class GetDownloadsUseCase(
    private val repository: MediaRepository
) {
    operator fun invoke(profileId: String): Flow<List<DownloadItem>> {
        return repository.observeDownloads(profileId)
    }
}

class GetDownloadUseCase(
    private val repository: MediaRepository
) {
    suspend operator fun invoke(mediaItemId: String, profileId: String): DownloadItem? {
        return repository.getDownload(mediaItemId, profileId)
    }
}

class SaveDownloadUseCase(
    private val repository: MediaRepository
) {
    suspend operator fun invoke(download: DownloadItem): Result<Unit> {
        return try {
            repository.saveDownload(download)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.error(e)
        }
    }
}

class UpdateDownloadProgressUseCase(
    private val repository: MediaRepository
) {
    suspend operator fun invoke(id: String, progress: Float, status: DownloadStatus): Result<Unit> {
        return try {
            repository.updateDownloadProgress(id, progress, status)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.error(e)
        }
    }
}

class DeleteDownloadUseCase(
    private val repository: MediaRepository
) {
    suspend operator fun invoke(id: String): Result<Unit> {
        return try {
            repository.deleteDownload(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.error(e)
        }
    }
}

class PauseDownloadUseCase(
    private val updateDownloadProgressUseCase: UpdateDownloadProgressUseCase
) {
    suspend operator fun invoke(id: String): Result<Unit> {
        return updateDownloadProgressUseCase(id, 0f, DownloadStatus.PAUSED)
    }
}

class ResumeDownloadUseCase(
    private val updateDownloadProgressUseCase: UpdateDownloadProgressUseCase
) {
    suspend operator fun invoke(id: String): Result<Unit> {
        return updateDownloadProgressUseCase(id, 0f, DownloadStatus.DOWNLOADING)
    }
}