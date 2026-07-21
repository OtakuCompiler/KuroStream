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

import com.kurostream.core.common.dispatcher.TestDispatcherProvider
import com.kurostream.core.common.result.Result
import com.kurostream.domain.entity.MediaItem
import com.kurostream.domain.entity.MediaType
import com.kurostream.domain.entity.AiringStatus
import com.kurostream.domain.entity.Season
import com.kurostream.domain.entity.ContentRating
import com.kurostream.domain.legacy.repository.MediaRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class SearchAnimeTest {

    private val mediaRepository: MediaRepository = mockk()
    private val dispatcherProvider = TestDispatcherProvider()

    private val useCase = SearchAnime(mediaRepository, dispatcherProvider)

    @Test
    fun `invoke returns search results`() = runTest {
        val query = "attack on titan"
        val mediaItems = listOf(
            MediaItem("1", "Attack on Titan", "Shingeki no Kyojin", "Action anime", "poster.jpg", "banner.jpg", MediaType.TV, AiringStatus.FINISHED, 25, 75, 24, 2013, Season.SPRING, listOf("Action", "Drama"), listOf("Wit Studio"), ContentRating.R17, 9.5, "anilist", "deeplink"),
            MediaItem("2", "Attack on Titan: The Final Season", "Shingeki no Kyojin: The Final Season", "Final season", "poster2.jpg", "banner2.jpg", MediaType.TV, AiringStatus.FINISHED, 25, 16, 24, 2020, Season.WINTER, listOf("Action", "Drama"), listOf("MAPPA"), ContentRating.R17, 9.8, "anilist", "deeplink")
        )

        coEvery { mediaRepository.search(query, 1, 25) } returns Result.Success(mediaItems)

        val results = useCase(query).toList()

        assertEquals(2, results.size)
        assertTrue(results[0].isLoading)
        assertTrue(results[1].isSuccess)
        assertEquals(mediaItems, results[1].getOrNull())
    }

    @Test
    fun `invoke uses default page and limit`() = runTest {
        val query = "naruto"

        coEvery { mediaRepository.search(query, 1, 25) } returns Result.Success(emptyList())

        val results = useCase(query).toList()

        assertEquals(2, results.size)
    }

    @Test
    fun `invoke propagates error from repository`() = runTest {
        val query = "error query"
        val exception = Exception("Search failed")

        coEvery { mediaRepository.search(query, 1, 25) } returns Result.Error(exception)

        val results = useCase(query).toList()

        assertTrue(results[1].isError)
        assertEquals(exception, results[1].exceptionOrNull())
    }
}
