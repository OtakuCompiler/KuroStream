// This file is part of KuroStream.
//
// NetflixMetadataProvider — TMDB watch-provider-backed Netflix catalog.
//
// SPDX-License-Identifier: GPL-3.0-only
package com.kurostream.data.metadata

import com.kurostream.data.remote.api.TmdbApi
import com.kurostream.domain.repository.CacheRepository
import javax.inject.Inject

class NetflixMetadataProvider @Inject constructor(
    api: TmdbApi,
    cache: CacheRepository,
) : TmdbWatchProviderMetadataProvider(
    api = api,
    cache = cache,
    providerId = "netflix",
    providerName = "Netflix",
    priority = 5,
    watchProviderId = "8",
)
