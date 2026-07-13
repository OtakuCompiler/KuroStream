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
import com.kurostream.domain.legacy.repository.MediaRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.*
import org.junit.Test

class GetTrendingTest {

    private val mediaRepository: MediaRepository = mockk()
    private val dispatcherProvider = TestDispatcherProvider

    private val useCase = GetTrending(mediaRepository, dispatcherProvider)

    @Test
    fun `invoke returns trending media`() = runBlockingTest {
        val trending = listOf(
            MediaItem("1", "Jujutsu Kaisen", "Jujutsu Kaisen", "Cursed energy battles", "poster.jpg", "banner.jpg", MediaItem.MediaType.TV, MediaItem.AiringStatus.FINISHED, 24, 24, 23, 2020, MediaItem.Season.FALL, listOf("Action", "Supernatural"), listOf("MAPPA"), MediaItem.ContentRating.R17, 9.2, "anilist", "deeplink"),
            MediaItem("2", "Demon Slayer", "Kimetsu no Yaiba", "Tanjiro's journey", "poster2.jpg", "banner2.jpg", MediaItem.MediaType.TV, MediaItem.AiringStatus.FINISHED, 26, 26, 24, 2019, MediaItem.Season.SPRING, listOf("Action", "Historical"), listOf("Ufotable"), MediaItem.ContentRating.R17, 9.0, "anilist", "deeplink")
        )

        coEvery { mediaRepository.getTrending() } returns flow { emit(Result.Loading); emit(Result.Success(trending)) }

        val results = useCase().toList()

        assertEquals(2, results.size)
        assertTrue(results[1].isSuccess)
        assertEquals(trending, results[1].getOrNull())
    }

    @Test
    fun `invoke propagates error from repository`() = runBlockingTest {
        val exception = Exception("Failed to fetch trending")

        coEvery { mediaRepository.getTrending() } returns flow { emit(Result.Loading); emit(Result.Error(exception)) }

        val results = useCase().toList()

        assertTrue(results[1].isError)
        assertEquals(exception, results[1].exceptionOrNull())
    }
}