// This file is part of KuroStream.
//
// StreamSource — unified source representation.
// Used by KuroStreamResolver to rank and present playback options.
//
// SPDX-License-Identifier: GPL-3.0-only
package com.kurostream.domain.resolver

import com.kurostream.domain.platform.HdrType
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
    /** Optional extended codec metadata. Populated by the probe layer
     *  for direct-play vs transcode decisions. */
    val metadata: StreamMetadata = StreamMetadata(),
)

@Serializable
data class StreamMetadata(
    val videoCodec: String? = null,
    val videoProfile: String? = null,
    val videoBitDepth: Int? = null,
    val width: Int? = null,
    val height: Int? = null,
    val frameRate: Double? = null,
    val hdrType: HdrType = HdrType.NONE,
    val audioCodec: String? = null,
    val audioChannels: Int? = null,
    val audioSampleRateHz: Int? = null,
    val audioTrackCount: Int = 1,
    val subtitleTrackCount: Int = 0,
    val container: String? = null,
)

enum class SourceType { TORRENT, HTTP, LOCAL, EXTENSION, DEBRID }
enum class SourceHealth { EXCELLENT, GOOD, DEGRADED, POOR, UNKNOWN }
