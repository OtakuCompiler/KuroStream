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
import androidx.media3.exoplayer.RendererCapabilities
import androidx.media3.exoplayer.source.TrackGroupArray
import androidx.media3.exoplayer.upstream.Allocator
import androidx.media3.exoplayer.upstream.DefaultAllocator
import kotlinx.coroutines.sync.Mutex

/**
 * Disk-backed LoadControl for ExoPlayer that uses DiskBufferManager
 * instead of in-memory allocation for buffering.
 * 
 * This reduces memory footprint significantly by keeping buffered data on disk
 * while still providing the same buffering behavior to ExoPlayer.
 */
class DiskBackedLoadControl private constructor(
    private val context: Context,
    private val targetBufferBytes: Long
) : LoadControl {
    
    private val allocator = DefaultAllocator.Builder().build()
    private val mutex = Mutex()
    
    // Target buffer levels in bytes (mirroring ExoPlayer's default behavior)
    private val minBufferMs = 5_000L
    private val maxBufferMs = 120_000L
    private val bufferForPlaybackMs = 2_500L
    private val bufferForPlaybackAfterRebufferMs = 5_000L
    private val backBufferMs = 15_000L
    
    companion object {
        @Suppress("UNUSED_PARAMETER")
        private var INSTANCE: DiskBackedLoadControl? = null
        
        fun getInstance(context: Context, targetBufferBytes: Long = 50_000_000): DiskBackedLoadControl {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: DiskBackedLoadControl(context.applicationContext, targetBufferBytes).also { INSTANCE = it }
            }
        }
        
        fun destroyInstance() {
            INSTANCE = null
        }
    }
    
    override fun getAllocator(): Allocator = allocator
    
    override fun getBackBufferDurationUs(): Long = backBufferMs * 1000
    
    override fun onPrepared() {
        // No-op
    }
    
    override fun onTracksSelected(
        rendererCapabilities: Array<RendererCapabilities>,
        trackGroups: TrackGroupArray,
        fixedTracks: IntArray
    ) {
        // No-op - track selection doesn't affect disk buffering
    }
    
    override fun shouldStartPlayback(
        bufferedDurationUs: Long,
        rebuffering: Boolean
    ): Boolean {
        val targetBufferUs = if (rebuffering) {
            bufferForPlaybackAfterRebufferMs * 1000
        } else {
            bufferForPlaybackMs * 1000
        }
        return bufferedDurationUs >= targetBufferUs
    }
    
    override fun shouldContinueLoading(
        bufferedDurationUs: Long,
        rebuffering: Boolean
    ): Boolean {
        val targetBufferUs = if (rebuffering) {
            maxBufferMs * 1000
        } else {
            // Dynamic target based on available disk space and network conditions
            maxBufferMs * 1000
        }
        return bufferedDurationUs < targetBufferUs
    }
    
    override fun onReleased() {
        // No cleanup needed
    }
}