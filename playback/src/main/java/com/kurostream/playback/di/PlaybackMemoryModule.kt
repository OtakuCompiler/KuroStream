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

package com.kurostream.playback.di

import com.kurostream.playback.memory.AdaptivePrebufferManager
import com.kurostream.playback.memory.CompressedFrameCache
import com.kurostream.playback.memory.KuroStreamMemoryManager
import com.kurostream.playback.memory.MemoryAwarePlaybackController
import com.kurostream.playback.p2p.OptimizedP2PEngine
import com.kurostream.playback.memory.ThermalQualityController
import com.kurostream.playback.memory.UltraLowMemoryManagerV3
import com.kurostream.playback.memory.YuvFramePool
import com.kurostream.playback.memory.ZeroCopyBufferManager
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object PlaybackMemoryModule {

    @Provides
    @Singleton
    fun provideMemoryAwarePlaybackController(
        memoryManager: KuroStreamMemoryManager,
        optimizedP2PEngine: OptimizedP2PEngine,
        thermalQualityController: ThermalQualityController,
        adaptivePrebufferManager: AdaptivePrebufferManager,
    ): MemoryAwarePlaybackController {
        return MemoryAwarePlaybackController(
            memoryManager = memoryManager,
            optimizedP2PEngine = optimizedP2PEngine,
            thermalQualityController = thermalQualityController,
            adaptivePrebufferManager = adaptivePrebufferManager,
        )
    }
}