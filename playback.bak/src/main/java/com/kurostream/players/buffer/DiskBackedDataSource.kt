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

package com.kurostream.players.buffer

import android.content.Context
import androidx.media3.exoplayer.upstream.DataSource
import androidx.media3.exoplayer.upstream.DataSpec
import java.nio.ByteBuffer

/**
 * Disk-backed DataSource that writes downloaded data to DiskBufferManager
 * while also providing it to ExoPlayer.
 */
class DiskBackedDataSource(
    private val upstream: DataSource,
    private val diskBufferManager: DiskBufferManager
) : DataSource {

    override fun open(dataSpec: DataSpec): Long {
        return upstream.open(dataSpec)
    }

    override fun read(buffer: ByteBuffer, length: Int): Int {
        val result = upstream.read(buffer, length)

        // If data was read successfully, also write to disk buffer
        if (result > 0 && diskBufferManager != null) {
            val data = java.util.Arrays.copyOfRange(buffer.array(), buffer.position(), buffer.position() + result)
            kotlinx.coroutines.Dispatchers.IO.execute {
                diskBufferManager?.write(data)
            }
        }

        return result
    }

    override fun close() {
        upstream.close()
    }

    override fun getUri(): android.net.Uri? {
        return upstream.getUri()
    }

    override fun getResponseHeaders(): Map<String, List<String>> {
        return upstream.getResponseHeaders()
    }
}

/**
 * Factory for creating DiskBackedDataSource instances.
 */
class DiskBackedDataSourceFactory(
    private val upstreamFactory: androidx.media3.exoplayer.upstream.DataSource.Factory,
    private val diskBufferManager: DiskBufferManager
) : androidx.media3.exoplayer.upstream.DataSource.Factory {

    override fun createDataSource(): DataSource {
        val upstream = upstreamFactory.createDataSource()
        return DiskBackedDataSource(upstream, diskBufferManager)
    }
}