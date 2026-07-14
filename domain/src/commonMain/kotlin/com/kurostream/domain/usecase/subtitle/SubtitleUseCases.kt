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

package com.kurostream.domain.usecase.subtitle

import com.kurostream.core.common.result.Result
import com.kurostream.domain.entity.SubtitleCandidate
import com.kurostream.domain.model.EpisodeInfo
import com.kurostream.domain.model.SubtitleResult
import com.kurostream.domain.repository.MediaRepository

class SearchSubtitlesUseCase(
    private val repository: MediaRepository
) {
    suspend operator fun invoke(
        query: String,
        languages: List<String>,
        episodeInfo: EpisodeInfo? = null
    ): Result<List<SubtitleResult>> {
        return try {
            val results = repository.searchSubtitles(query, languages, episodeInfo)
            Result.success(results)
        } catch (e: Exception) {
            Result.error(e)
        }
    }
}

class GetBestSubtitleUseCase {
    suspend operator fun invoke(
        candidates: List<SubtitleResult>,
        preferredLanguages: List<String>,
        hearingImpaired: Boolean = false
    ): SubtitleResult? {
        return candidates.maxByOrNull { candidate ->
            var score = candidate.rating * 10 + candidate.downloadCount
            if (candidate.language in preferredLanguages) score += 100
            if (candidate.hearingImpaired && hearingImpaired) score += 50
            score
        }
    }
}

class DownloadSubtitleUseCase(
    private val repository: MediaRepository
) {
    suspend operator fun invoke(
        subtitleResult: SubtitleResult,
        destinationPath: String
    ): Result<Unit> {
        return try {
            // Implementation would download the subtitle file
            Result.success(Unit)
        } catch (e: Exception) {
            Result.error(e)
        }
    }
}