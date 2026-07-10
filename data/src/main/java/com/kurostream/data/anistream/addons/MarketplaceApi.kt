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

package com.kurostream.data.anistream.addons

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MarketplaceApi @Inject constructor(
    private val httpClient: HttpClient
) {
    companion object {
        const val BASE_URL = "https://anistream-marketplace.example.com/api"
    }

    suspend fun getAddons(): List<com.kurostream.legacyui.anistream.ui.addons.MarketplaceAddon> {
        return try {
            val response = httpClient.get("$BASE_URL/addons")
            response.body<List<MarketplaceAddonResponse>>().map { it.toMarketplaceAddon() }
        } catch (e: Exception) {
            // Return demo data for testing
            getDemoAddons()
        }
    }

    private fun getDemoAddons(): List<com.kurostream.legacyui.anistream.ui.addons.MarketplaceAddon> {
        return listOf(
            com.kurostream.legacyui.anistream.ui.addons.MarketplaceAddon(
                id = "demo_1",
                name = "Crunchyroll Source",
                version = "1.0.0",
                description = "Stream from Crunchyroll catalog",
                author = "AniStream Team",
                rating = 4.5f,
                downloadCount = 15000,
                iconUrl = null,
                screenshotUrls = emptyList(),
                permissions = listOf("INTERNET"),
                sourceUrl = "https://example.com/addons/crunchyroll.zip",
                isOfficial = true
            ),
            com.kurostream.legacyui.anistream.ui.addons.MarketplaceAddon(
                id = "demo_2",
                name = "AniList Tracker",
                version = "2.1.0",
                description = "Advanced AniList integration with recommendations",
                author = "Community",
                rating = 4.2f,
                downloadCount = 8200,
                iconUrl = null,
                screenshotUrls = emptyList(),
                permissions = listOf("INTERNET", "SYNC"),
                sourceUrl = "https://example.com/addons/anilist.zip",
                isOfficial = false
            )
        )
    }
}

@Serializable
data class MarketplaceAddonResponse(
    val id: String,
    val name: String,
    val version: String,
    val description: String,
    val author: String,
    val rating: Float,
    val downloadCount: Int,
    val iconUrl: String? = null,
    val screenshotUrls: List<String> = emptyList(),
    val permissions: List<String> = emptyList(),
    val sourceUrl: String,
    val isOfficial: Boolean = false
) {
    fun toMarketplaceAddon() = com.kurostream.legacyui.anistream.ui.addons.MarketplaceAddon(
        id = id, name = name, version = version, description = description,
        author = author, rating = rating, downloadCount = downloadCount,
        iconUrl = iconUrl, screenshotUrls = screenshotUrls,
        permissions = permissions, sourceUrl = sourceUrl, isOfficial = isOfficial
    )
}
