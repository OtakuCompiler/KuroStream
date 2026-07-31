// This file is part of KuroStream.
//
// StreamSource — unified source representation.
// Used by KuroStreamResolver to rank and present playback options.
//
// SPDX-License-Identifier: GPL-3.0-only
package com.kurostream.domain.resolver

import kotlinx.serialization.Serializable

@Serializable
data class StreamSource(
    val id: String,
    val name: String,
    val url: String,
    val quality: String,
    val codec: String,
    val isHdr: Boolean = false,
    val isDolbyVision: Boolean = false,
    val audio: String = "",
    val subtitleCandidates: List<String> = emptyList(),
    val sourceType: SourceType,
    val providerName: String,
    val sizeBytes: Long? = null,
    val seeders: Int? = null,
    val health: SourceHealth = SourceHealth.UNKNOWN,
)

enum class SourceType { TORRENT, HTTP, LOCAL, EXTENSION, DEBRID }
enum class SourceHealth { EXCELLENT, GOOD, DEGRADED, POOR, UNKNOWN }
