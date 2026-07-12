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

import com.kurostream.core.common.result.Result
import com.kurostream.plugin.sdk.marketplace.model.ExtensionListing
import com.kurostream.plugin.sdk.marketplace.model.Review
import com.kurostream.plugin.sdk.marketplace.model.ReviewSummary
import kotlinx.coroutines.flow.Flow

interface MarketplaceRepository {
    fun observeFeatured(): Flow<Result<List<ExtensionListing>>>
    fun observeByCategory(category: String): Flow<Result<List<ExtensionListing>>>
    suspend fun search(query: String, page: Int, limit: Int): Result<List<ExtensionListing>>
    suspend fun getListing(extensionId: String): Result<ExtensionListing>
    suspend fun getReviews(extensionId: String, page: Int, limit: Int): Result<List<Review>>
    suspend fun getReviewSummary(extensionId: String): Result<ReviewSummary>
    suspend fun submitReview(review: Review): Result<Review>
    suspend fun initiatePurchase(extensionId: String): Result<String>
    suspend fun verifyPurchase(extensionId: String, token: String): Result<String>
}
