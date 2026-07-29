package com.kurostream.domain.repository

interface DownloadRepository {
    suspend fun getDownloads(): List<String>
    suspend fun download(mediaId: String, url: String)
}
