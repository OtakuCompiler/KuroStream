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
import com.kurostream.domain.entity.AnimeDetails
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

class GetAnimeDetailsTest {

    private val mediaRepository: MediaRepository = mockk()
    private val dispatcherProvider = TestDispatcherProvider()

    private val useCase = GetAnimeDetails(mediaRepository, dispatcherProvider)

    @Test
    fun `invoke returns anime details`() = runTest {
        val mediaId = "anime123"
        val mediaItem = MediaItem(
            id = mediaId,
            title = "Fullmetal Alchemist: Brotherhood",
            originalTitle = "Fullmetal Alchemist: Brotherhood",
            synopsis = "Two brothers search for the Philosopher's Stone",
            coverImageUrl = "cover.jpg",
            bannerImageUrl = "banner.jpg",
            type = MediaType.TV,
            status = AiringStatus.FINISHED,
            episodeNumber = 1,
            totalEpisodes = 64,
            durationMinutes = 24,
            seasonYear = 2009,
            seasonQuarter = Season.SPRING,
            genres = listOf("Action", "Adventure", "Fantasy"),
            studios = listOf("Bones"),
            rating = ContentRating.R17,
            score = 9.9,
            sourceExtensionId = "anilist",
            deepLink = "deeplink"
        )
        val details = AnimeDetails(
            mediaItem = mediaItem,
            relatedAnime = emptyList(),
            characters = emptyList(),
            staff = emptyList()
        )

        coEvery { mediaRepository.getAnimeDetails(mediaId) } returns Result.Success(details)

        val results = useCase(mediaId).toList()

        assertEquals(2, results.size)
        assertTrue(results[1].isSuccess)
        assertEquals(details, results[1].getOrNull())
    }

    @Test
    fun `invoke propagates error from repository`() = runTest {
        val mediaId = "notfound"
        val exception = Exception("Anime not found")

        coEvery { mediaRepository.getAnimeDetails(mediaId) } returns Result.Error(exception)

        val results = useCase(mediaId).toList()

        assertTrue(results[1].isError)
        assertEquals(exception, results[1].exceptionOrNull())
    }
}
