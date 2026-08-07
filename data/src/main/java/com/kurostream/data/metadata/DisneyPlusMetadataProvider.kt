// This file is part of KuroStream.
//
// DisneyPlusMetadataProvider — TMDB watch-provider-backed Disney+ catalog.
//
// SPDX-License-Identifier: GPL-3.0-only
package com.kurostream.data.metadata

import com.kurostream.data.remote.api.TmdbApi
import com.kurostream.domain.repository.CacheRepository
import javax.inject.Inject

class DisneyPlusMetadataProvider @Inject constructor(
    api: TmdbApi,
    cache: CacheRepository,
) : TmdbWatchProviderMetadataProvider(
    api = api,
    cache = cache,
    providerId = "disneyplus",
    providerName = "Disney+",
    priority = 6,
    watchProviderId = "337",
)
