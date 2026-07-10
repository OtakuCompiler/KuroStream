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

package com.kurostream.domain.legacy.usecase

import com.kurostream.common.dispatcher.DispatcherProvider
import com.kurostream.common.result.Result
import com.kurostream.domain.entity.MediaItem
import com.kurostream.domain.legacy.repository.MediaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

class SearchAnime @Inject constructor(
    private val mediaRepository: MediaRepository,
    private val dispatcherProvider: DispatcherProvider
) {
    operator fun invoke(query: String, page: Int = 1, limit: Int = 25): Flow<Result<List<MediaItem>>> {
        return flow { emit(Result.Loading); emit(mediaRepository.search(query, page, limit)) }.flowOn(dispatcherProvider.io)
    }
}
