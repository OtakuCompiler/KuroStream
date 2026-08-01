package com.kurostream.domain.entity

import kotlinx.serialization.Serializable

@Serializable
data class VideoSource(
    val url: String,
    val quality: String = "Unknown",
    val headers: Map<String, String> = emptyMap(),
    val isTorrent: Boolean = false,
    val torrentHash: String? = null,
    val torrentFileIndex: Int? = null,
    val subtitleUrls: List<String> = emptyList(),
    val sizeBytes: Long? = null,
)
