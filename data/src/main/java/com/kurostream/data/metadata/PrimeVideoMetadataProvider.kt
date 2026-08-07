// This file is part of KuroStream.
//
// PrimeVideoMetadataProvider — TMDB watch-provider-backed Prime Video catalog.
//
// SPDX-License-Identifier: GPL-3.0-only
package com.kurostream.data.metadata

import com.kurostream.data.remote.api.TmdbApi
import com.kurostream.domain.repository.CacheRepository
import javax.inject.Inject

class PrimeVideoMetadataProvider @Inject constructor(
    api: TmdbApi,
    cache: CacheRepository,
) : TmdbWatchProviderMetadataProvider(
    api = api,
    cache = cache,
    providerId = "primevideo",
    providerName = "Prime Video",
    priority = 6,
    watchProviderId = "9",
)
