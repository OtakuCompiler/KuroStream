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

package com.kurostream.plugin.sdk.marketplace.model

import com.kurostream.domain.entity.ExtensionCapability
import com.kurostream.domain.entity.SemanticVersion
import kotlinx.serialization.Serializable

@Serializable
data class ExtensionListing(
    val id: String,
    val name: String,
    val author: String,
    val shortDescription: String,
    val fullDescription: String? = null,
    val iconUrl: String? = null,
    val bannerUrl: String? = null,
    val screenshots: List<String> = emptyList(),
    val version: SemanticVersion,
    val latestVersion: SemanticVersion? = null,
    val capabilities: Set<ExtensionCapability> = emptySet(),
    val category: MarketplaceCategory,
    val pricing: PricingModel,
    val downloadUrl: String? = null,
    val packageSizeBytes: Long = 0L,
    val installCount: Long = 0L,
    val averageRating: Float = 0f,
    val reviewCount: Int = 0,
    val reviews: List<Review> = emptyList(),
    val isOfficial: Boolean = false,
    val isVerified: Boolean = false,
    val minAppVersion: SemanticVersion = SemanticVersion(1, 0, 0),
    val lastUpdated: Long = System.currentTimeMillis(),
    val releaseNotes: String? = null
)

@Serializable
data class PricingModel(
    val type: PricingType,
    val basePriceCents: Int? = null,
    val salePriceCents: Int? = null,
    val currency: String = "USD",
    val trialDays: Int? = null,
    val subscriptionPeriod: SubscriptionPeriod? = null
)

enum class PricingType { FREE, PAID, SUBSCRIPTION, FREEMIUM }
enum class SubscriptionPeriod { MONTHLY, YEARLY, LIFETIME }
enum class MarketplaceCategory { CATALOG, SOURCE, TRACKING, THEME, UTILITY, LANGUAGE_PACK, PREMIUM_BUNDLE }
