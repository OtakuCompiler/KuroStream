package com.kurostream.data.download

import android.content.Context
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.scheduler.Requirements
import java.util.concurrent.Executors

class KuroDownloadManager(context: Context) {

    private val downloadExecutor = Executors.newFixedThreadPool(2)
    private val downloadManager: DownloadManager

    init {
        val databaseProvider = androidx.media3.database.StandaloneDatabaseProvider(context)
        val downloadCache = androidx.media3.datasource.cache.SimpleCache(
            File(context.getExternalFilesDir(null), "downloads"),
            androidx.media3.datasource.cache.NoOpCacheEvictor(),
            databaseProvider
        )
        val upstreamFactory = androidx.media3.datasource.DefaultHttpDataSource.Factory()
        downloadManager = DownloadManager(
            context,
            databaseProvider,
            downloadCache,
            upstreamFactory,
            downloadExecutor
        )
    }

    fun startDownload(mediaId: String, uri: String) {
        val request = DownloadRequest.Builder(mediaId, android.net.Uri.parse(uri)).build()
        downloadManager.addDownload(request)
    }

    fun removeDownload(mediaId: String) {
        downloadManager.removeDownload(mediaId)
    }

    fun getDownloads(): List<Download> = downloadManager.currentDownloads

    fun release() {
        downloadManager.release()
        downloadExecutor.shutdown()
    }
}
