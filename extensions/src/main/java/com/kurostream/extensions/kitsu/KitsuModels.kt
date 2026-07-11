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

package com.kurostream.extensions.kitsu

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class KitsuAnimeListResponse(
    @Json(name = "data") val data: List<KitsuAnimeData>,
    @Json(name = "included") val included: List<KitsuIncluded>? = null,
    @Json(name = "meta") val meta: KitsuMeta? = null
)

@JsonClass(generateAdapter = true)
data class KitsuAnimeResponse(
    @Json(name = "data") val data: KitsuAnimeData,
    @Json(name = "included") val included: List<KitsuIncluded>? = null
)

@JsonClass(generateAdapter = true)
data class KitsuAnimeData(
    @Json(name = "id") val id: String,
    @Json(name = "type") val type: String,
    @Json(name = "attributes") val attributes: KitsuAnimeAttributes,
    @Json(name = "relationships") val relationships: KitsuRelationships? = null
)

@JsonClass(generateAdapter = true)
data class KitsuAnimeAttributes(
    @Json(name = "slug") val slug: String? = null,
    @Json(name = "synopsis") val synopsis: String? = null,
    @Json(name = "canonicalTitle") val canonicalTitle: String? = null,
    @Json(name = "titles") val titles: KitsuTitles? = null,
    @Json(name = "abbreviatedTitles") val abbreviatedTitles: List<String>? = null,
    @Json(name = "averageRating") val averageRating: String? = null,
    @Json(name = "ratingFrequencies") val ratingFrequencies: Map<String, String>? = null,
    @Json(name = "userCount") val userCount: Int? = null,
    @Json(name = "favoritesCount") val favoritesCount: Int? = null,
    @Json(name = "startDate") val startDate: String? = null,
    @Json(name = "endDate") val endDate: String? = null,
    @Json(name = "nextRelease") val nextRelease: String? = null,
    @Json(name = "popularityRank") val popularityRank: Int? = null,
    @Json(name = "ratingRank") val ratingRank: Int? = null,
    @Json(name = "ageRating") val ageRating: String? = null,
    @Json(name = "ageRatingGuide") val ageRatingGuide: String? = null,
    @Json(name = "subtype") val subtype: String? = null,
    @Json(name = "status") val status: String? = null,
    @Json(name = "tba") val tba: String? = null,
    @Json(name = "posterImage") val posterImage: KitsuImageSet? = null,
    @Json(name = "coverImage") val coverImage: KitsuImageSet? = null,
    @Json(name = "episodeCount") val episodeCount: Int? = null,
    @Json(name = "episodeLength") val episodeLength: Int? = null,
    @Json(name = "totalLength") val totalLength: Int? = null,
    @Json(name = "showType") val showType: String? = null,
    @Json(name = "nsfw") val nsfw: Boolean? = null
)

@JsonClass(generateAdapter = true)
data class KitsuTitles(
    @Json(name = "en") val en: String? = null,
    @Json(name = "en_jp") val enJp: String? = null,
    @Json(name = "ja_jp") val jaJp: String? = null
)

@JsonClass(generateAdapter = true)
data class KitsuImageSet(
    @Json(name = "tiny") val tiny: String? = null,
    @Json(name = "small") val small: String? = null,
    @Json(name = "medium") val medium: String? = null,
    @Json(name = "large") val large: String? = null,
    @Json(name = "original") val original: String? = null,
    @Json(name = "meta") val meta: ImageMeta? = null
)

@JsonClass(generateAdapter = true)
data class ImageMeta(
    @Json(name = "dimensions") val dimensions: Map<String, ImageDimensions>? = null
)

@JsonClass(generateAdapter = true)
data class ImageDimensions(
    @Json(name = "width") val width: Int? = null,
    @Json(name = "height") val height: Int? = null
)

@JsonClass(generateAdapter = true)
data class KitsuRelationships(
    @Json(name = "genres") val genres: KitsuRelationship? = null,
    @Json(name = "characters") val characters: KitsuRelationship? = null,
    @Json(name = "staff") val staff: KitsuRelationship? = null,
    @Json(name = "castings") val castings: KitsuRelationship? = null
)

@JsonClass(generateAdapter = true)
data class KitsuRelationship(
    @Json(name = "data") val data: List<KitsuRelationshipData>? = null,
    @Json(name = "links") val links: KitsuLinks? = null
)

@JsonClass(generateAdapter = true)
data class KitsuRelationshipData(
    @Json(name = "id") val id: String,
    @Json(name = "type") val type: String
)

@JsonClass(generateAdapter = true)
data class KitsuLinks(
    @Json(name = "self") val self: String? = null,
    @Json(name = "related") val related: String? = null
)

@JsonClass(generateAdapter = true)
data class KitsuIncluded(
    @Json(name = "id") val id: String,
    @Json(name = "type") val type: String,
    @Json(name = "attributes") val attributes: KitsuIncludedAttributes? = null
)

@JsonClass(generateAdapter = true)
data class KitsuIncludedAttributes(
    @Json(name = "name") val name: String? = null,
    @Json(name = "slug") val slug: String? = null,
    @Json(name = "description") val description: String? = null,
    @Json(name = "image") val image: KitsuImageSet? = null,
    @Json(name = "role") val role: String? = null,
    @Json(name = "voiceActor") val voiceActor: Boolean? = null,
    @Json(name = "featured") val featured: Boolean? = null,
    @Json(name = "canonicalName") val canonicalName: String? = null
)

@JsonClass(generateAdapter = true)
data class KitsuMeta(
    @Json(name = "count") val count: Int? = null
)

@JsonClass(generateAdapter = true)
data class KitsuCharacterListResponse(
    @Json(name = "data") val data: List<KitsuCharacterData>
)

@JsonClass(generateAdapter = true)
data class KitsuCharacterResponse(
    @Json(name = "data") val data: KitsuCharacterData
)

@JsonClass(generateAdapter = true)
data class KitsuCharacterData(
    @Json(name = "id") val id: String,
    @Json(name = "type") val type: String,
    @Json(name = "attributes") val attributes: KitsuCharacterAttributes? = null
)

@JsonClass(generateAdapter = true)
data class KitsuCharacterAttributes(
    @Json(name = "name") val name: String? = null,
    @Json(name = "description") val description: String? = null,
    @Json(name = "image") val image: KitsuImageSet? = null,
    @Json(name = "otherNames") val otherNames: List<String>? = null,
    @Json(name = "canonicalName") val canonicalName: String? = null
)

@JsonClass(generateAdapter = true)
data class KitsuStaffListResponse(
    @Json(name = "data") val data: List<KitsuStaffData>
)

@JsonClass(generateAdapter = true)
data class KitsuStaffData(
    @Json(name = "id") val id: String,
    @Json(name = "type") val type: String,
    @Json(name = "attributes") val attributes: KitsuStaffAttributes? = null
)

@JsonClass(generateAdapter = true)
data class KitsuStaffAttributes(
    @Json(name = "name") val name: String? = null,
    @Json(name = "description") val description: String? = null,
    @Json(name = "image") val image: KitsuImageSet? = null,
    @Json(name = "role") val role: String? = null
)
