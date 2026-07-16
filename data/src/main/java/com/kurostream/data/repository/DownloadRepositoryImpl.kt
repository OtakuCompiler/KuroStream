package com.kurostream.data.repository

import com.kurostream.domain.repository.DownloadRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadRepositoryImpl @Inject constructor() : DownloadRepository {
    override suspend fun getDownloads(): List<String> = emptyList()
    override suspend fun download(mediaId: String, url: String) {}
}
