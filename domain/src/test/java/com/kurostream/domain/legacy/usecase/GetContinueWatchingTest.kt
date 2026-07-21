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
import com.kurostream.domain.legacy.repository.ProfileRepository
import com.kurostream.domain.model.Profile
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class GetContinueWatchingTest {

    private val mediaRepository: MediaRepository = mockk()
    private val profileRepository: ProfileRepository = mockk()
    private val dispatcherProvider = TestDispatcherProvider()

    private val useCase = GetContinueWatching(mediaRepository, profileRepository, dispatcherProvider)

    @Test
    fun `invoke returns continue watching items`() = runTest {
        val profileId = "profile1"
        val continueWatching = listOf(
            MediaItem("1", "One Piece", "One Piece", "Pirate adventure", "poster.jpg", "banner.jpg", MediaType.TV, AiringStatus.AIRING, 1000, 1100, 24, 1999, Season.FALL, listOf("Adventure", "Fantasy"), listOf("Toei Animation"), ContentRating.R17, 9.5, "anilist", "deeplink"),
            MediaItem("2", "Naruto", "Naruto", "Ninja adventure", "poster2.jpg", "banner2.jpg", MediaType.TV, AiringStatus.FINISHED, 220, 220, 23, 2002, Season.FALL, listOf("Action", "Adventure"), listOf("Pierrot"), ContentRating.R17, 9.1, "anilist", "deeplink")
        )

        coEvery { profileRepository.observeActiveProfile() } returns flowOf<Profile?>(Profile(id = profileId, displayName = "Test", isPremium = false))
        coEvery { mediaRepository.getTrending(1, 20) } returns Result.Success(continueWatching)

        val results = useCase().toList()

        assertEquals(2, results.size)
        assertTrue(results[0].isLoading)
        assertTrue(results[1].isSuccess)
        assertEquals(continueWatching, results[1].getOrNull())
    }

    @Test
    fun `invoke returns empty when no active profile`() = runTest {
        coEvery { profileRepository.observeActiveProfile() } returns flowOf<Profile?>(null)

        val results = useCase().toList()

        assertEquals(1, results.size)
        assertTrue(results[0].isSuccess)
        assertEquals(emptyList<MediaItem>(), results[0].getOrNull())
    }
}
