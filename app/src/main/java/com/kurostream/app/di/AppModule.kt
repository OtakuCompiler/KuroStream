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
import com.kurostream.app.startup.KuroStreamInitializer
import com.kurostream.app.player.PlayerInitializer
import com.kurostream.app.extensions.PluginScannerInitializer
import com.kurostream.app.repository.SyncInitializer
import com.kurostream.app.optimization.BatteryAwareManager
import com.kurostream.app.optimization.StartupProfiler
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

    // App Startup initializers
    @Provides
    @Singleton
    fun provideKuroStreamInitializer(): KuroStreamInitializer = KuroStreamInitializer()

    @Provides
    @Singleton
    fun providePluginScannerInitializer(): PluginScannerInitializer = PluginScannerInitializer()

    @Provides
    @Singleton
    fun providePlayerInitializer(): PlayerInitializer = PlayerInitializer()

    @Provides
    @Singleton
    fun provideSyncInitializer(): SyncInitializer = SyncInitializer()

    @Provides
    @Singleton
    fun provideNetworkDashboardViewModel(): NetworkDashboardViewModel = NetworkDashboardViewModel()
}