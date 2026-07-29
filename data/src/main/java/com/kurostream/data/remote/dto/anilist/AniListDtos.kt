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

package com.kurostream.data.remote.dto.anilist

import com.google.gson.annotations.SerializedName

data class AniListGraphQLRequest(val query: String, val variables: Map<String, Any>? = null)

data class AniListGraphQLResponse(val data: Map<String, Any>?, val errors: List<AniListError>?)

data class AniListError(val message: String, val status: Int?)

data class AniListSearchRequest(
    val query: String = """
        query SearchAnime(${'$'}search: String, ${'$'}page: Int, ${'$'}perPage: Int) {
            Page(page: ${'$'}page, perPage: ${'$'}perPage) {
                media(search: ${'$'}search, type: ANIME) {
                    id
                    title { romaji english native }
                    coverImage { large medium }
                    bannerImage
                    description
                    episodes
                    duration
                    averageScore
                    genres
                    status
                    season
                    seasonYear
                }
            }
        }
    """.trimIndent(),
    val variables: Map<String, Any>
)

data class AniListSearchResponse(val data: AniListSearchData?)
data class AniListSearchData(val Page: AniListPage?)
data class AniListPage(val media: List<AniListMedia>?)

data class AniListMedia(
    val id: Int,
    val title: AniListTitle?,
    val coverImage: AniListCoverImage?,
    val bannerImage: String?,
    val description: String?,
    val episodes: Int?,
    val duration: Int?,
    val averageScore: Int?,
    val genres: List<String>?,
    val status: String?,
    val season: String?,
    val seasonYear: Int?
)

data class AniListTitle(val romaji: String?, val english: String?, val native: String?)
data class AniListCoverImage(val large: String?, val medium: String?)

data class AniListAnimeDetailsRequest(
    val query: String = """
        query GetAnimeDetails(${'$'}id: Int) {
            Media(id: ${'$'}id, type: ANIME) {
                id
                title { romaji english native }
                coverImage { large medium }
                bannerImage
                description
                episodes
                duration
                averageScore
                genres
                status
                startDate { year month day }
                endDate { year month day }
                studios { edges { node { name } } }
                characters { edges { node { name { full } image { medium } } } }
            }
        }
    """.trimIndent(),
    val variables: Map<String, Any>
)

data class AniListAnimeDetailsResponse(val data: AniListAnimeDetailsData?)
data class AniListAnimeDetailsData(val Media: AniListMedia?)

data class AniListTrendingRequest(
    val query: String = """
        query GetTrendingAnime(${'$'}page: Int, ${'$'}perPage: Int) {
            Page(page: ${'$'}page, perPage: ${'$'}perPage) {
                media(sort: TRENDING_DESC, type: ANIME) {
                    id
                    title { romaji english }
                    coverImage { large }
                    bannerImage
                    description
                    averageScore
                    genres
                }
            }
        }
    """.trimIndent(),
    val variables: Map<String, Any> = mapOf("page" to 1, "perPage" to 20)
)

data class AniListTrendingResponse(val data: AniListSearchData?)
