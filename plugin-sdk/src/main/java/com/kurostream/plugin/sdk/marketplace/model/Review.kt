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

import kotlinx.serialization.Serializable

@Serializable
data class Review(
    val id: String,
    val extensionId: String,
    val authorName: String,
    val authorAvatarUrl: String? = null,
    val rating: Int,
    val title: String? = null,
    val body: String,
    val versionAtReview: String? = null,
    val helpfulCount: Int = 0,
    val isVerifiedPurchase: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long? = null
) {
    init { require(rating in 1..5) { "Rating must be between 1 and 5" } }
}

@Serializable
data class ReviewSummary(
    val extensionId: String,
    val averageRating: Float,
    val totalReviews: Int,
    val distribution: Map<Int, Int>
)
