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

package com.kurostream.app.di

import com.kurostream.common.thermal.ThermalGuard
import com.kurostream.app.network.NetworkDashboardViewModel
import com.kurostream.common.optimization.BatteryAwareManager
import com.kurostream.common.optimization.StartupProfiler
import com.kurostream.playback.memory.AdaptivePrebufferManager
import com.kurostream.playback.memory.CompressedFrameCache
import com.kurostream.playback.memory.KuroStreamMemoryManager
import com.kurostream.playback.memory.OptimizedP2PEngine
import com.kurostream.playback.memory.ThermalQualityController
import com.kurostream.playback.memory.UltraLowMemoryManagerV3
import com.kurostream.playback.memory.YuvFramePool
import com.kurostream.playback.memory.ZeroCopyBufferManager
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.Module
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object AppModule {

    @Provides
    @Singleton
    fun provideThermalGuard(@ApplicationContext context: Context): ThermalGuard {
        return ThermalGuard(context)
    }

    @Provides
    @Singleton
    fun provideBatteryAwareManager(
        @ApplicationContext context: Context,
    ): BatteryAwareManager {
        return BatteryAwareManager.create(context)
    }

    @Provides
    @Singleton
    fun provideStartupProfiler(): StartupProfiler {
        return StartupProfiler()
    }

    @Provides
    @Singleton
    fun provideNetworkDashboardViewModel(): NetworkDashboardViewModel = NetworkDashboardViewModel()

    // Ultra-Low RAM Memory Management Components
    @Provides
    @Singleton
    fun provideUltraLowMemoryManagerV3(
        @ApplicationContext context: Context,
    ): UltraLowMemoryManagerV3 {
        return UltraLowMemoryManagerV3(context)
    }

    @Provides
    @Singleton
    fun provideZeroCopyBufferManager(): ZeroCopyBufferManager {
        return ZeroCopyBufferManager()
    }

    @Provides
    @Singleton
    fun provideYuvFramePool(): YuvFramePool {
        return YuvFramePool()
    }

    @Provides
    @Singleton
    fun provideCompressedFrameCache(): CompressedFrameCache {
        return CompressedFrameCache()
    }

    @Provides
    @Singleton
    fun provideAdaptivePrebufferManager(): AdaptivePrebufferManager {
        return AdaptivePrebufferManager()
    }

    @Provides
    @Singleton
    fun provideThermalQualityController(
        @ApplicationContext context: Context,
    ): ThermalQualityController {
        return ThermalQualityController(context)
    }

    @Provides
    @Singleton
    fun provideOptimizedP2PEngine(
        bufferManager: ZeroCopyBufferManager,
        prebufferManager: AdaptivePrebufferManager
    ): OptimizedP2PEngine {
        return OptimizedP2PEngine(bufferManager, prebufferManager)
    }

    @Provides
    @Singleton
    fun provideKuroStreamMemoryManager(
        ultraLowMemoryManager: UltraLowMemoryManagerV3,
        zeroCopyBufferManager: ZeroCopyBufferManager,
        yuvFramePool: YuvFramePool,
        compressedFrameCache: CompressedFrameCache,
        adaptivePrebufferManager: AdaptivePrebufferManager,
        thermalQualityController: ThermalQualityController,
        optimizedP2PEngine: OptimizedP2PEngine
    ): KuroStreamMemoryManager {
        return KuroStreamMemoryManager(
            ultraLowMemoryManager,
            zeroCopyBufferManager,
            yuvFramePool,
            compressedFrameCache,
            adaptivePrebufferManager,
            thermalQualityController,
            optimizedP2PEngine
        )
    }
}