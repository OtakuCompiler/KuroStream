package com.kurostream.app.repository

import com.kurostream.app.model.MediaItem
import com.kurostream.domain.entity.MediaItem as DomainMediaItem
import com.kurostream.domain.model.Favorite
import com.kurostream.domain.model.Profile
import com.kurostream.domain.repository.MediaRepository
import com.kurostream.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FavoritesRepositoryBridgeTest {

    @Test
    fun `getFavorites returns empty list when no active profile`() = runTest {
        val mediaRepo = FakeMediaRepository()
        val profileRepo = FakeProfileRepository(activeProfile = null)
        val bridge = FavoritesRepositoryBridge(mediaRepo, profileRepo)

        val favorites = bridge.getFavorites().first()
        assertTrue(favorites.isEmpty())
    }

    @Test
    fun `addFavorite and getFavorites persist via domain repository`() = runTest {
        val mediaItem = MediaItem(
            id = "media1",
            title = "Test Anime",
            description = "Desc",
            posterUrl = "",
            backdropUrl = "",
            genre = emptyList(),
            rating = 0f,
            year = 2024,
            duration = 24,
            episodes = emptyList(),
            source = "",
            isFavorite = false,
            watchProgress = 0L
        )
        val domainMediaItem = DomainMediaItem(
            id = "media1",
            title = "Test Anime",
            description = "Desc",
            posterUrl = "",
            backdropUrl = "",
            genre = emptyList(),
            rating = 0f,
            year = 2024,
            duration = 24,
            source = "",
            isFavorite = true
        )
        val profile = Profile(id = "profile1", displayName = "Test", avatarUrl = null)
        val mediaRepo = FakeMediaRepository(mediaById = mapOf("media1" to domainMediaItem))
        val profileRepo = FakeProfileRepository(activeProfile = profile)
        val bridge = FavoritesRepositoryBridge(mediaRepo, profileRepo)

        bridge.addFavorite(mediaItem)
        val favorites = bridge.getFavorites().first()
        assertEquals(1, favorites.size)
        assertEquals("media1", favorites.first().id)
    }

    @Test
    fun `removeFavorite removes via domain repository`() = runTest {
        val domainMediaItem = DomainMediaItem(
            id = "media1",
            title = "Test Anime",
            description = "Desc",
            posterUrl = "",
            backdropUrl = "",
            genre = emptyList(),
            rating = 0f,
            year = 2024,
            duration = 24,
            source = "",
            isFavorite = true
        )
        val profile = Profile(id = "profile1", displayName = "Test", avatarUrl = null)
        val mediaRepo = FakeMediaRepository(mediaById = mapOf("media1" to domainMediaItem))
        val profileRepo = FakeProfileRepository(activeProfile = profile)
        val bridge = FavoritesRepositoryBridge(mediaRepo, profileRepo)

        bridge.addFavorite(
            MediaItem(
                id = "media1",
                title = "Test Anime",
                description = "Desc",
                posterUrl = "",
                backdropUrl = "",
                genre = emptyList(),
                rating = 0f,
                year = 2024,
                duration = 24,
                episodes = emptyList(),
                source = "",
                isFavorite = false,
                watchProgress = 0L
            )
        )
        var favorites = bridge.getFavorites().first()
        assertEquals(1, favorites.size)

        bridge.removeFavorite("media1")
        favorites = bridge.getFavorites().first()
        assertTrue(favorites.isEmpty())
    }

    private class FakeMediaRepository(
        private val mediaById: Map<String, DomainMediaItem> = emptyMap(),
        private val favorites: MutableList<Favorite> = mutableListOf()
    ) : MediaRepository {
        override suspend fun getMediaItems(): List<String> = emptyList()
        override suspend fun getMediaItem(id: String): String? = null
        override fun observeAllMediaItems(): Flow<List<DomainMediaItem>> = flowOf(emptyList())
        override suspend fun search(query: String): List<String> = emptyList()
        override suspend fun searchLocal(query: String): List<DomainMediaItem> = emptyList()
        override suspend fun searchRemote(query: String, source: String?): List<DomainMediaItem> = emptyList()
        override suspend fun getTrending(source: String?): List<DomainMediaItem> = emptyList()
        override suspend fun getRemoteDetails(mediaId: String, source: String): DomainMediaItem? = null
        override suspend fun saveMediaItem(item: DomainMediaItem) {}
        override suspend fun saveMediaItems(items: List<DomainMediaItem>) {}
        override suspend fun deleteMediaItem(id: String) {}
        override fun observeMediaByCategory(category: com.kurostream.domain.model.MediaCategory): Flow<List<DomainMediaItem>> = flowOf(emptyList())
        override suspend fun getMediaById(id: String): DomainMediaItem? = mediaById[id]
        override fun observeFavorites(profileId: String): Flow<List<Favorite>> = flowOf(favorites.toList())
        override suspend fun isFavorite(mediaItemId: String, profileId: String): Boolean = favorites.any { it.mediaItemId == mediaItemId && it.profileId == profileId }
        override suspend fun addFavorite(favorite: Favorite) {
            favorites.add(favorite)
        }
        override suspend fun removeFavorite(mediaItemId: String, profileId: String) {
            favorites.removeAll { it.mediaItemId == mediaItemId && it.profileId == profileId }
        }
        override fun observeWatchHistory(profileId: String): Flow<List<com.kurostream.domain.model.WatchHistory>> = flowOf(emptyList())
        override suspend fun getWatchHistory(mediaItemId: String, profileId: String): com.kurostream.domain.model.WatchHistory? = null
        override suspend fun saveWatchHistory(history: com.kurostream.domain.model.WatchHistory) {}
        override suspend fun deleteWatchHistory(mediaItemId: String, profileId: String) {}
        override suspend fun searchSubtitles(query: String, languages: List<String>, episodeInfo: com.kurostream.domain.model.EpisodeInfo?): List<com.kurostream.domain.model.SubtitleResult> = emptyList()
        override suspend fun getPlaybackUrl(mediaId: String, episodeId: String?): com.kurostream.domain.result.Result<com.kurostream.domain.model.PlaybackUrl> = com.kurostream.domain.result.Result.error(IllegalStateException("not implemented"))
        override suspend fun getNextEpisode(mediaId: String, episodeId: String?): com.kurostream.domain.result.Result<com.kurostream.domain.model.EpisodeInfo> = com.kurostream.domain.result.Result.error(IllegalStateException("not implemented"))
    }

    private class FakeProfileRepository(
        private val activeProfile: Profile?
    ) : ProfileRepository {
        override fun observeAllProfiles(): Flow<List<Profile>> = flowOf(emptyList())
        override fun observeActiveProfile(): Flow<Profile?> = flowOf(activeProfile)
        override suspend fun getActiveProfile(): Profile? = activeProfile
        override suspend fun getProfileById(id: String): Profile? = null
        override suspend fun createProfile(name: String, avatarUrl: String?, pin: String?): com.kurostream.domain.result.Result<Profile> = com.kurostream.domain.result.Result.error(IllegalStateException("not implemented"))
        override suspend fun updateProfile(id: String, name: String?, avatarUrl: String?): com.kurostream.domain.result.Result<Profile> = com.kurostream.domain.result.Result.error(IllegalStateException("not implemented"))
        override suspend fun switchProfile(profileId: String): com.kurostream.domain.result.Result<Profile> = com.kurostream.domain.result.Result.error(IllegalStateException("not implemented"))
        override suspend fun deleteProfile(id: String): com.kurostream.domain.result.Result<Unit> = com.kurostream.domain.result.Result.error(IllegalStateException("not implemented"))
        override suspend fun setPin(profileId: String, pin: String): com.kurostream.domain.result.Result<Unit> = com.kurostream.domain.result.Result.error(IllegalStateException("not implemented"))
        override suspend fun removePin(profileId: String): com.kurostream.domain.result.Result<Unit> = com.kurostream.domain.result.Result.error(IllegalStateException("not implemented"))
        override suspend fun verifyPin(profileId: String, pin: String): Boolean = false
        override suspend fun hasPin(profileId: String): Boolean = false
        override suspend fun updatePreferences(profileId: String, preferencesJson: String): com.kurostream.domain.result.Result<Unit> = com.kurostream.domain.result.Result.error(IllegalStateException("not implemented"))
        override suspend fun getPreferences(profileId: String): String? = null
        override suspend fun getProfiles(): List<Profile> = emptyList()
        override suspend fun getProfile(profileId: String): Profile? = null
        override suspend fun saveProfile(profile: Profile): com.kurostream.domain.result.Result<Unit> = com.kurostream.domain.result.Result.error(IllegalStateException("not implemented"))
        override suspend fun setActiveProfile(profileId: String): com.kurostream.domain.result.Result<Unit> = com.kurostream.domain.result.Result.error(IllegalStateException("not implemented"))
    }
}
