package com.kurostream.domain.debrid

import kotlinx.serialization.Serializable

@Serializable
data class RdAvailabilityRequest(
    val hashes: List<String>,
)

@Serializable
data class RdAvailabilityResponse(
    val rd: Map<String, RdFileInfo>,
)

@Serializable
data class RdFileInfo(
    val filename: String,
    val filesize: Long,
    val id: String,
)

@Serializable
data class RdTorrentRequest(
    val magnet: String,
    val host: String = "real-debrid.com",
)

@Serializable
data class RdTorrentResponse(
    val id: String,
    val filename: String,
    val status: String,
)

@Serializable
data class RdTorrentInfo(
    val id: String,
    val filename: String,
    val status: String,
    val progress: Float,
    val links: List<String> = emptyList(),
)

@Serializable
data class RdUnrestrictedLinkRequest(
    val link: String,
)

@Serializable
data class RdUnrestrictedLinkResponse(
    val id: String,
    val filename: String,
    val filesize: Long,
    val download: String,
    val link: String,
)

@Serializable
data class RdUser(
    val id: Int,
    val username: String,
    val email: String,
    val points: Int,
    val avatar: String,
    val type: String,
    val expiration: Long,
)
