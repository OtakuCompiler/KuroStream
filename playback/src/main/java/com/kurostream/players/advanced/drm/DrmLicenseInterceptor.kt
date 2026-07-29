// This file is part of KuroStream.
//
// KuroStream is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// KuroStream is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with KuroStream.  If not, see <https://www.gnu.org/licenses/>.

package com.kurostream.players.advanced.drm

import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.TransferListener
import java.io.IOException
import java.io.InputStream
import java.net.URL

/**
 * DataSource interceptor that routes DRM license requests through the internal proxy.
 * Integrates with ExoPlayer's DefaultDrmSessionManager.
 */
class DrmLicenseInterceptor(
    private val proxyServer: WidevineProxyServer,
    private val upstreamFactory: DataSource.Factory
) : DataSource {

    private var upstream: DataSource? = null
    private var currentDataSpec: DataSpec? = null

    override fun open(dataSpec: DataSpec): Long {
        currentDataSpec = dataSpec

        // Check if this is a license request
        val isLicenseRequest = dataSpec.uri.toString().contains("license") ||
                dataSpec.uri.toString().contains("widevine")

        return if (isLicenseRequest) {
            // Route through proxy
            val proxyUri = dataSpec.uri.buildUpon()
                .scheme("http")
                .authority("127.0.0.1:${proxyServer.listeningPort}")
                .path(WidevineProxyServer.PROXY_PATH_LICENSE)
                .build()

            val proxyDataSpec = dataSpec.buildUpon().setUri(proxyUri).build()
            val source = upstreamFactory.createDataSource()
            upstream = source
            source.open(proxyDataSpec)
        } else {
            // Pass through
            val source = upstreamFactory.createDataSource()
            upstream = source
            source.open(dataSpec)
        }
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        return upstream?.read(buffer, offset, length) ?: throw IOException("DataSource not open")
    }

    override fun addTransferListener(transferListener: TransferListener) {
        upstream?.addTransferListener(transferListener)
    }

    override fun getUri() = upstream?.getUri()

    override fun getResponseHeaders() = upstream?.getResponseHeaders() ?: emptyMap()

    override fun close() {
        upstream?.close()
        upstream = null
    }
}
