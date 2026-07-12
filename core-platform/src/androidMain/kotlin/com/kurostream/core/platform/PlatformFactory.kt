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

package com.kurostream.core.platform

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

actual class PlatformFactory(
    private val context: Context,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
) {
    actual fun createPlayer(): PlatformPlayer = AndroidPlayer(context, scope)
    actual fun createStorage(): PlatformStorage = AndroidStorage(context, scope)
    actual fun createNetwork(): PlatformNetwork = AndroidNetwork(context)
    actual fun createUI(): PlatformUI = AndroidUI(context)
    
    actual companion object {
        private var INSTANCE: PlatformFactory? = null
        @Suppress("UNUSED_PARAMETER")
        actual fun getInstance(): PlatformFactory {
            return INSTANCE ?: throw IllegalStateException("PlatformFactory not initialized. Call init(context) first.")
        }
        
        fun init(context: Context) {
            if (INSTANCE == null) {
                INSTANCE = PlatformFactory(context.applicationContext)
            }
        }
    }
}