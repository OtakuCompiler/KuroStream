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

import com.kurostream.common.dispatcher.DefaultDispatcherProvider
import com.kurostream.common.result.Result
import com.kurostream.domain.entity.HomeRow
import com.kurostream.domain.legacy.repository.MediaRepository
import com.kurostream.domain.legacy.repository.ProfileRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.*
import org.junit.Test

class GetHomeRowsTest {

    private val mediaRepository: MediaRepository = mockk()
    private val profileRepository: ProfileRepository = mockk()
    private val dispatcherProvider = DefaultDispatcherProvider()

    private val useCase = GetHomeRows(mediaRepository, profileRepository, dispatcherProvider)

    @Test
    fun `invoke returns home rows for active profile`() = runBlockingTest {
        val profileId = "profile1"
        val homeRows = listOf(
            HomeRow("Trending", listOf()),
            HomeRow("Continue Watching", listOf())
        )

        coEvery { profileRepository.observeActiveProfile() } returns flowOf(Profile(profileId, "Test", false))
        coEvery { mediaRepository.observeHomeRows(profileId) } returns flowOf(Result.Success(homeRows))

        val results = useCase().toList()

        assertEquals(1, results.size)
        assertTrue(results[0].isSuccess)
        assertEquals(homeRows, results[0].getOrNull())
    }

    @Test
    fun `invoke returns empty list when no active profile`() = runBlockingTest {
        coEvery { profileRepository.observeActiveProfile() } returns flowOf(null)

        val results = useCase().toList()

        assertEquals(1, results.size)
        assertTrue(results[0].isSuccess)
        assertEquals(emptyList(), results[0].getOrNull())
    }

    @Test
    fun `invoke propagates error from media repository`() = runBlockingTest {
        val profileId = "profile1"
        val exception = Exception("Network error")

        coEvery { profileRepository.observeActiveProfile() } returns flowOf(Profile(profileId, "Test", false))
        coEvery { mediaRepository.observeHomeRows(profileId) } returns flowOf(Result.Error(exception))

        val results = useCase().toList()

        assertTrue(results[0].isError)
        assertEquals(exception, results[0].exceptionOrNull())
    }
}

data class Profile(
    val id: String,
    val displayName: String,
    val isPremium: Boolean
)