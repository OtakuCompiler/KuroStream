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

package com.kurostream.plugin.sdk.marketplace

import com.kurostream.common.result.Result
import com.kurostream.domain.entity.ExtensionCapability
import com.kurostream.domain.entity.SemanticVersion
import com.kurostream.plugin.sdk.marketplace.model.ExtensionListing
import com.kurostream.plugin.sdk.marketplace.model.MarketplaceCategory
import com.kurostream.plugin.sdk.marketplace.model.PricingModel
import com.kurostream.plugin.sdk.marketplace.model.PricingType
import com.kurostream.plugin.sdk.marketplace.model.Review
import com.kurostream.plugin.sdk.marketplace.model.ReviewSummary
import com.kurostream.plugin.sdk.marketplace.model.SubscriptionPeriod
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

class DemoMarketplaceRepository @Inject constructor() : MarketplaceRepository {

    private val demoListings = listOf(
        ExtensionListing(
            id = "premium-anilist-sync", name = "AniList Sync Pro", author = "KuroStream Premium",
            shortDescription = "Two-way sync with AniList. Auto-track progress, scores, and lists.",
            fullDescription = "Keep your AniList profile perfectly in sync with KuroStream. Automatically update episode progress, ratings, and list entries. Supports custom lists, rewatch tracking, and advanced filtering.",
            iconUrl = "https://marketplace.kurostream.app/icons/anilist-pro.png",
            version = SemanticVersion(2, 1, 0),
            capabilities = setOf(ExtensionCapability.TRACKING, ExtensionCapability.SYNC_PROVIDER),
            category = MarketplaceCategory.TRACKING,
            pricing = PricingModel(type = PricingType.SUBSCRIPTION, basePriceCents = 499, subscriptionPeriod = SubscriptionPeriod.MONTHLY, trialDays = 7),
            installCount = 45_230, averageRating = 4.7f, reviewCount = 1_203, isOfficial = true, isVerified = true
        ),
        ExtensionListing(
            id = "catalog-hidive", name = "HIDIVE Catalog", author = "KuroStream Premium",
            shortDescription = "Access the full HIDIVE streaming catalog with official subtitles.",
            version = SemanticVersion(1, 5, 2),
            capabilities = setOf(ExtensionCapability.CATALOG_BROWSE, ExtensionCapability.VIDEO_SOURCE, ExtensionCapability.SUBTITLE_SOURCE),
            category = MarketplaceCategory.CATALOG,
            pricing = PricingModel(type = PricingType.PAID, basePriceCents = 999, salePriceCents = 699),
            installCount = 12_450, averageRating = 4.2f, reviewCount = 342, isOfficial = true, isVerified = true
        ),
        ExtensionListing(
            id = "theme-oled-dark", name = "OLED Pure Black Theme", author = "Community",
            shortDescription = "True black theme optimized for OLED TVs. Reduces eye strain and saves power.",
            version = SemanticVersion(1, 0, 0), capabilities = emptySet(),
            category = MarketplaceCategory.THEME,
            pricing = PricingModel(type = PricingType.FREE),
            installCount = 89_100, averageRating = 4.9f, reviewCount = 2_100, isOfficial = false, isVerified = true
        ),
        ExtensionListing(
            id = "lang-pack-spanish", name = "Spanish Language Pack", author = "Comunidad KuroStream",
            shortDescription = "Full Spanish UI translation and curated Spanish subtitle sources.",
            version = SemanticVersion(3, 2, 0), capabilities = setOf(ExtensionCapability.SUBTITLE_SOURCE),
            category = MarketplaceCategory.LANGUAGE_PACK,
            pricing = PricingModel(type = PricingType.FREE),
            installCount = 34_500, averageRating = 4.5f, reviewCount = 876, isOfficial = false, isVerified = false
        )
    )

    private val demoReviews = listOf(
        Review(id = "rev-001", extensionId = "premium-anilist-sync", authorName = "AnimeFan99", rating = 5,
            title = "Worth every penny", body = "The auto-tracking is flawless. Never have to manually update AniList again.",
            versionAtReview = "2.0.0", helpfulCount = 45, isVerifiedPurchase = true),
        Review(id = "rev-002", extensionId = "premium-anilist-sync", authorName = "CasualWatcher", rating = 4,
            body = "Great extension but the sync is sometimes delayed by a few minutes.",
            versionAtReview = "2.1.0", helpfulCount = 12, isVerifiedPurchase = true),
        Review(id = "rev-003", extensionId = "theme-oled-dark", authorName = "OLED_Gamer", rating = 5,
            title = "Perfect for my LG C3", body = "Finally a true black theme that doesn't crush shadows. Looks amazing.",
            helpfulCount = 89, isVerifiedPurchase = false)
    )

    override fun observeFeatured(): Flow<Result<List<ExtensionListing>>> = flowOf(Result.Success(demoListings.filter { it.isOfficial }))
    override fun observeByCategory(category: String): Flow<Result<List<ExtensionListing>>> = flowOf(Result.Success(demoListings.filter { it.category.name.equals(category, ignoreCase = true) }))
    override suspend fun search(query: String, page: Int, limit: Int): Result<List<ExtensionListing>> = Result.Success(demoListings.filter { it.name.contains(query, ignoreCase = true) || it.shortDescription.contains(query, ignoreCase = true) || it.author.contains(query, ignoreCase = true) })
    override suspend fun getListing(extensionId: String): Result<ExtensionListing> {
        val listing = demoListings.find { it.id == extensionId } ?: return Result.Error(IllegalArgumentException("Listing not found: $extensionId"))
        return Result.Success(listing.copy(reviews = demoReviews.filter { it.extensionId == extensionId }))
    }
    override suspend fun getReviews(extensionId: String, page: Int, limit: Int): Result<List<Review>> = Result.Success(demoReviews.filter { it.extensionId == extensionId })
    override suspend fun getReviewSummary(extensionId: String): Result<ReviewSummary> {
        val reviews = demoReviews.filter { it.extensionId == extensionId }
        val avg = if (reviews.isNotEmpty()) reviews.map { it.rating }.average().toFloat() else 0f
        return Result.Success(ReviewSummary(extensionId = extensionId, averageRating = avg, totalReviews = reviews.size, distribution = reviews.groupingBy { it.rating }.eachCount()))
    }
    override suspend fun submitReview(review: Review): Result<Review> = Result.Success(review)
    override suspend fun initiatePurchase(extensionId: String): Result<String> = Result.Success("demo-token-${System.currentTimeMillis()}")
    override suspend fun verifyPurchase(extensionId: String, token: String): Result<String> = Result.Success("https://marketplace.kurostream.app/download/$extensionId?token=$token")
}
