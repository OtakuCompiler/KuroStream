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
import androidx.media3.exoplayer.LoadControl
import androidx.media3.exoplayer.upstream.Allocator
import androidx.media3.exoplayer.upstream.DefaultAllocator

/**
 * Disk-backed allocator for ExoPlayer that uses DiskBufferManager
 * instead of in-memory allocation.
 * 
 * This reduces RAM usage by storing buffered data on disk.
 */
class DiskBackedAllocator private constructor(
    private val context: Context,
    private val targetBufferBytes: Long
) : Allocator {

    private var diskBufferManager: DiskBufferManager? = null
    
    companion object {
        @Suppress("UNUSED_PARAMETER")
        private var INSTANCE: DiskBackedAllocator? = null
        
        fun getInstance(context: Context, targetBufferBytes: Long): DiskBackedAllocator {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: DiskBackedAllocator(context.applicationContext, targetBufferBytes).also { INSTANCE = it }
            }
        }
        
        fun destroyInstance() {
            INSTANCE?.release()
            INSTANCE = null
        }
    }
    
    init {
        // Initialize disk buffer asynchronously
        kotlinx.coroutines.Dispatchers.IO.execute {
            try {
                val config = DiskBufferManager.DiskBufferConfig(
                    bufferSizeMb = (targetBufferBytes / 1024 / 1024).coerceAtLeast(100).toInt(),
                    maxReadAheadMb = 4,
                    averageBitrateBps = 15_000_000
                )
                diskBufferManager = DiskBufferManager.getInstance(context, config)
                val result = diskBufferManager.initialize()
                if (result.isFailure) {
                    diskBufferManager = null
                }
            } catch (e: Exception) {
                diskBufferManager = null
            }
        }
    }
    
    override fun allocate(size: Int): java.nio.ByteBuffer {
        // Return a direct byte buffer - data will be written to disk buffer
        // The actual data transfer happens in the DataSource
        return java.nio.ByteBuffer.allocateDirect(size)
    }
    
    override fun release(buffer: java.nio.ByteBuffer) {
        // Buffers are managed by DiskBufferManager
        // No-op for disk-backed approach
    }
    
    override fun getIndividualAllocationLength(): Int {
        return targetBufferBytes.coerceAtMost(Int.MAX_VALUE).toInt()
    }
    
    override fun getTotalBytesAllocated(): Long {
        return diskBufferManager?.getUnreadBytes() ?: 0
    }
    
    override fun trim(targetSize: Long) {
        if (diskBufferManager != null) {
            val unread = diskBufferManager!!.getUnreadBytes()
            val toTrim = (unread - targetSize).coerceAtLeast(0)
            if (toTrim > 0) {
                kotlinx.coroutines.Dispatchers.IO.execute {
                    diskBufferManager?.trim(toTrim)
                }
            }
        }
    }
    
    fun release() {
        diskBufferManager?.shutdown()
        diskBufferManager = null
    }
}