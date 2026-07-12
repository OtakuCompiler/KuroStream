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

package com.kurostream.domain.legacy.repository

import com.kurostream.core.common.result.Result
import com.kurostream.domain.entity.AnimeDetails
import com.kurostream.domain.entity.Episode
import com.kurostream.domain.entity.HomeRow
import com.kurostream.domain.entity.MediaItem
import com.kurostream.domain.entity.SubtitleCandidate
import com.kurostream.domain.entity.VideoSource
import kotlinx.coroutines.flow.Flow

interface MediaRepository {
    fun observeHomeRows(profileId: String): Flow<Result<List<HomeRow>>>
    suspend fun getTrending(page: Int, limit: Int): Result<List<MediaItem>>
    suspend fun search(query: String, page: Int, limit: Int): Result<List<MediaItem>>
    suspend fun getAnimeDetails(mediaId: String): Result<AnimeDetails>
    suspend fun getEpisodes(mediaId: String): Result<List<Episode>>
    suspend fun getVideoSources(episodeId: String): Result<List<VideoSource>>
    suspend fun getSubtitleCandidates(episodeId: String): Result<List<SubtitleCandidate>>
}
