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
}