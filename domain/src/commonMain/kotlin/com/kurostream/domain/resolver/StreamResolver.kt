// This file is part of KuroStream.
//
// StreamSourceExtensions — extension points for additional resolvers
// and health backends.
//
// SPDX-License-Identifier: GPL-3.0-only
package com.kurostream.domain.resolver

import kotlinx.coroutines.flow.Flow

interface StreamResolver {
    suspend fun resolve(query: String): List<StreamSource>
    fun observeHealth(): Flow<Map<String, SourceHealth>>
}
