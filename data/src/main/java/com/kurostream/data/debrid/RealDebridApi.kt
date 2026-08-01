package com.kurostream.data.debrid

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface RealDebridApi {
    @GET("user")
    suspend fun getUser(): RdUserResponse

    @POST("torrents/instantAvailability")
    suspend fun checkInstantAvailability(@Body request: RdAvailabilityRequest): RdAvailabilityResponse

    @POST("torrents/addMagnet")
    suspend fun addMagnet(@Body request: RdMagnetRequest): RdMagnetResponse

    @POST("torrents/addTorrent")
    suspend fun addTorrentFile(@Body request: RdTorrentFileRequest): RdMagnetResponse

    @GET("torrents/info/{id}")
    suspend fun getTorrentInfo(@retrofit2.http.Path("id") id: String): RdTorrentInfoResponse

    @POST("torrents/selectFiles/{id}")
    suspend fun selectFiles(@retrofit2.http.Path("id") id: String, @Body request: RdSelectFilesRequest)

    @POST("unrestrict/link")
    suspend fun unrestrictLink(@Body request: RdUnrestrictRequest): RdUnrestrictResponse

    @GET("torrents")
    suspend fun getTorrents(): List<RdTorrentInfoResponse>
}

data class RdUserResponse(
    val id: Int,
    val username: String,
    val email: String,
    val points: Int,
    val avatar: String,
    val type: String,
    val expiration: Long,
)

data class RdAvailabilityRequest(
    val hashes: List<String>,
)

data class RdAvailabilityResponse(
    val rd: Map<String, List<RdFileInfoResponse>>,
)

data class RdFileInfoResponse(
    val filename: String,
    val filesize: Long,
    val id: String,
)

data class RdMagnetRequest(
    val magnet: String,
    val host: String = "real-debrid.com",
)

data class RdMagnetResponse(
    val id: String,
    val filename: String,
)

data class RdTorrentFileRequest(
    val torrent: String,
    val host: String = "real-debrid.com",
)

data class RdSelectFilesRequest(
    val files: String,
)

data class RdUnrestrictRequest(
    val link: String,
)

data class RdUnrestrictResponse(
    val id: String,
    val filename: String,
    val filesize: Long,
    val download: String,
    val link: String,
)

data class RdTorrentInfoResponse(
    val id: String,
    val filename: String,
    val status: String,
    val progress: Float,
    val links: List<String> = emptyList(),
)

object Constants {
    const val REAL_DEBRID_BASE_URL = "https://api.real-debrid.com/rest/1.0/"
}
