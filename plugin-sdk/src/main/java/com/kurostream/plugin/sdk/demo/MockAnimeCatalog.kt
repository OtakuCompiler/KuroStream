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

package com.kurostream.plugin.sdk.demo

import com.kurostream.core.common.result.Result
import com.kurostream.domain.entity.AiringStatus
import com.kurostream.domain.entity.AnimeDetails
import com.kurostream.domain.entity.ContentRating
import com.kurostream.domain.entity.Episode
import com.kurostream.domain.entity.ExtensionCapability
import com.kurostream.domain.entity.ExtensionInfo
import com.kurostream.domain.entity.HomeRow
import com.kurostream.domain.entity.MediaItem
import com.kurostream.domain.entity.MediaType
import com.kurostream.domain.entity.RowType
import com.kurostream.domain.entity.Season
import com.kurostream.domain.entity.SemanticVersion
import com.kurostream.domain.entity.VideoQuality
import com.kurostream.domain.entity.VideoSource
import com.kurostream.plugin.sdk.api.ExtensionApi
import com.kurostream.plugin.sdk.api.ExtensionConfig

class MockAnimeCatalog : ExtensionApi {
    override val extensionId: String = "com.kurostream.demo.mock-catalog"
    override val info: ExtensionInfo = ExtensionInfo(
        id = extensionId,
        name = "Mock Anime Catalog",
        author = "KuroStream Team",
        version = SemanticVersion(1, 0, 0),
        description = "Built-in demo catalog with hardcoded anime data for testing and development.",
        packageName = "com.kurostream.plugin.sdk.demo",
        capabilities = setOf(
            ExtensionCapability.CATALOG_BROWSE,
            ExtensionCapability.CATALOG_SEARCH,
            ExtensionCapability.EPISODE_LIST,
            ExtensionCapability.VIDEO_SOURCE
        ),
        isInstalled = true,
        isEnabled = true,
        isTrusted = true
    )

    private val mockAnime = listOf(
        MediaItem(
            id = "mock-001",
            title = "Attack on Titan",
            originalTitle = "Shingeki no Kyojin",
            synopsis = "Humanity lives inside cities surrounded by enormous walls that protect them from gigantic man-eating humanoids referred to as Titans.",
            coverImageUrl = "https://cdn.myanimelist.net/images/anime/10/47347.jpg",
            bannerImageUrl = "https://cdn.myanimelist.net/images/anime/10/47347l.jpg",
            type = MediaType.TV,
            status = AiringStatus.FINISHED,
            totalEpisodes = 87,
            durationMinutes = 24,
            seasonYear = 2013,
            seasonQuarter = Season.SPRING,
            genres = listOf("Action", "Drama", "Fantasy"),
            studios = listOf("Wit Studio", "MAPPA"),
            rating = ContentRating.R17,
            score = 9.05,
            sourceExtensionId = extensionId
        ),
        MediaItem(
            id = "mock-002",
            title = "Steins;Gate",
            originalTitle = "Steins;Gate",
            synopsis = "A self-proclaimed mad scientist discovers a way to send messages to the past using a modified microwave and cell phone.",
            coverImageUrl = "https://cdn.myanimelist.net/images/anime/5/73199.jpg",
            bannerImageUrl = "https://cdn.myanimelist.net/images/anime/5/73199l.jpg",
            type = MediaType.TV,
            status = AiringStatus.FINISHED,
            totalEpisodes = 24,
            durationMinutes = 24,
            seasonYear = 2011,
            seasonQuarter = Season.SPRING,
            genres = listOf("Sci-Fi", "Thriller", "Drama"),
            studios = listOf("White Fox"),
            rating = ContentRating.PG13,
            score = 9.08,
            sourceExtensionId = extensionId
        ),
        MediaItem(
            id = "mock-003",
            title = "Demon Slayer",
            originalTitle = "Kimetsu no Yaiba",
            synopsis = "A young boy becomes a demon slayer after his family is slaughtered and his sister is turned into a demon.",
            coverImageUrl = "https://cdn.myanimelist.net/images/anime/1286/99889.jpg",
            bannerImageUrl = "https://cdn.myanimelist.net/images/anime/1286/99889l.jpg",
            type = MediaType.TV,
            status = AiringStatus.FINISHED,
            totalEpisodes = 26,
            durationMinutes = 24,
            seasonYear = 2019,
            seasonQuarter = Season.SPRING,
            genres = listOf("Action", "Supernatural", "Historical"),
            studios = listOf("ufotable"),
            rating = ContentRating.R17,
            score = 8.50,
            sourceExtensionId = extensionId
        ),
        MediaItem(
            id = "mock-004",
            title = "Your Name",
            originalTitle = "Kimi no Na wa.",
            synopsis = "Two teenagers share a profound, magical connection upon discovering they are swapping bodies.",
            coverImageUrl = "https://cdn.myanimelist.net/images/anime/5/87048.jpg",
            bannerImageUrl = "https://cdn.myanimelist.net/images/anime/5/87048l.jpg",
            type = MediaType.MOVIE,
            status = AiringStatus.FINISHED,
            durationMinutes = 106,
            seasonYear = 2016,
            seasonQuarter = Season.SUMMER,
            genres = listOf("Romance", "Supernatural", "Drama"),
            studios = listOf("CoMix Wave Films"),
            rating = ContentRating.PG13,
            score = 8.85,
            sourceExtensionId = extensionId
        ),
        MediaItem(
            id = "mock-005",
            title = "Jujutsu Kaisen",
            originalTitle = "Jujutsu Kaisen",
            synopsis = "A high school student swallows a cursed finger and becomes host to a powerful curse, joining a secret organization of Jujutsu Sorcerers.",
            coverImageUrl = "https://cdn.myanimelist.net/images/anime/1171/109222.jpg",
            bannerImageUrl = "https://cdn.myanimelist.net/images/anime/1171/109222l.jpg",
            type = MediaType.TV,
            status = AiringStatus.AIRING,
            totalEpisodes = 47,
            durationMinutes = 24,
            seasonYear = 2020,
            seasonQuarter = Season.FALL,
            genres = listOf("Action", "Supernatural", "School"),
            studios = listOf("MAPPA"),
            rating = ContentRating.R17,
            score = 8.60,
            sourceExtensionId = extensionId
        ),
        MediaItem(
            id = "mock-006",
            title = "Cyberpunk: Edgerunners",
            originalTitle = "Cyberpunk: Edgerunners",
            synopsis = "In a dystopia riddled with corruption and cybernetic implants, a talented but reckless street kid strives to become an edgerunner.",
            coverImageUrl = "https://cdn.myanimelist.net/images/anime/1818/126632.jpg",
            bannerImageUrl = "https://cdn.myanimelist.net/images/anime/1818/126632l.jpg",
            type = MediaType.TV,
            status = AiringStatus.FINISHED,
            totalEpisodes = 10,
            durationMinutes = 24,
            seasonYear = 2022,
            seasonQuarter = Season.SUMMER,
            genres = listOf("Action", "Sci-Fi", "Psychological"),
            studios = listOf("Trigger"),
            rating = ContentRating.R,
            score = 8.61,
            sourceExtensionId = extensionId
        )
    )

    private val mockEpisodes = mapOf(
        "mock-001" to listOf(
            Episode(
                id = "mock-001-ep1",
                mediaId = "mock-001",
                number = 1,
                title = "To You, in 2000 Years: The Fall of Shiganshina, Part 1",
                synopsis = "The citizens of Shiganshina are caught off guard when the Colossal Titan appears and destroys the outer wall.",
                thumbnailUrl = "https://cdn.myanimelist.net/images/anime/10/47347.jpg",
                durationSeconds = 1440,
                videoSources = listOf(
                    VideoSource(id = "vs-001-1", quality = VideoQuality.P1080, url = "https://demo.kurostream.app/mock/aot/ep1/1080.m3u8", isHls = true),
                    VideoSource(id = "vs-001-2", quality = VideoQuality.P720, url = "https://demo.kurostream.app/mock/aot/ep1/720.m3u8", isHls = true)
                )
            ),
            Episode(
                id = "mock-001-ep2",
                mediaId = "mock-001",
                number = 2,
                title = "That Day: The Fall of Shiganshina, Part 2",
                synopsis = "Eren, Mikasa, and Armin witness the horror of the Titans as they breach the wall.",
                thumbnailUrl = "https://cdn.myanimelist.net/images/anime/10/47347.jpg",
                durationSeconds = 1440,
                videoSources = listOf(
                    VideoSource(id = "vs-002-1", quality = VideoQuality.P1080, url = "https://demo.kurostream.app/mock/aot/ep2/1080.m3u8", isHls = true)
                )
            )
        ),
        "mock-002" to listOf(
            Episode(
                id = "mock-002-ep1",
                mediaId = "mock-002",
                number = 1,
                title = "Prologue to the Beginning and End",
                synopsis = "Rintaro Okabe attends a time travel lecture and discovers a dead body.",
                thumbnailUrl = "https://cdn.myanimelist.net/images/anime/5/73199.jpg",
                durationSeconds = 1440,
                videoSources = listOf(
                    VideoSource(id = "vs-003-1", quality = VideoQuality.P1080, url = "https://demo.kurostream.app/mock/sg/ep1/1080.m3u8", isHls = true)
                )
            )
        )
    )

    private var config: ExtensionConfig? = null

    override suspend fun onCreate(config: ExtensionConfig) {
        this.config = config
    }

    override suspend fun onEnable() {}

    override suspend fun onDisable() {}

    override suspend fun onDestroy() {
        config = null
    }

    override fun getCapabilities(): Set<ExtensionCapability> = info.capabilities

    override suspend fun getHomeRows(): Result<List<HomeRow>> = Result.Success(listOf(
        HomeRow(
            id = "row-trending",
            title = "Trending Now",
            type = RowType.TRENDING,
            items = mockAnime.take(4),
            actionLabel = "See All",
            actionRoute = "trending"
        ),
        HomeRow(
            id = "row-action",
            title = "Action Picks",
            type = RowType.CUSTOM_LIST,
            items = mockAnime.filter { "Action" in it.genres },
            actionLabel = "More Action",
            actionRoute = "genre/action"
        ),
        HomeRow(
            id = "row-movies",
            title = "Top Movies",
            type = RowType.CUSTOM_LIST,
            items = mockAnime.filter { it.type == MediaType.MOVIE }
        )
    ))

    override suspend fun search(query: String, page: Int, limit: Int): Result<List<MediaItem>> {
        val filtered = mockAnime.filter {
            it.title.contains(query, ignoreCase = true) ||
            it.originalTitle?.contains(query, ignoreCase = true) == true ||
            it.genres.any { g -> g.contains(query, ignoreCase = true) }
        }
        return Result.Success(filtered)
    }

    override suspend fun getAnimeDetails(mediaId: String): Result<AnimeDetails> {
        val media = mockAnime.find { it.id == mediaId } ?: return Result.Error(IllegalArgumentException("Anime not found: $mediaId"))
        val episodes = mockEpisodes[mediaId] ?: emptyList()
        val related = mockAnime.filter { it.id != mediaId && it.genres.intersect(media.genres.toSet()).isNotEmpty() }
        return Result.Success(AnimeDetails(mediaItem = media, episodes = episodes, relatedAnime = related.take(4)))
    }

    override suspend fun getVideoSources(episodeId: String): Result<List<VideoSource>> {
        val episode = mockEpisodes.values.flatten().find { it.id == episodeId } ?: return Result.Error(IllegalArgumentException("Episode not found: $episodeId"))
        return Result.Success(episode.videoSources)
    }

    override suspend fun getSubtitleCandidates(episodeId: String) = Result.Success(emptyList<com.kurostream.domain.entity.SubtitleCandidate>())

    override suspend fun reportProgress(mediaId: String, episodeNumber: Int, progressPercent: Float) = Result.Success(Unit)

    override suspend fun syncWatchlist() = Result.Success(emptyList<MediaItem>())
}