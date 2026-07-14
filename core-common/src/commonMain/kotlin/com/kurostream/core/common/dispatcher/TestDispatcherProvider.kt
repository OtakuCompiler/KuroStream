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

package com.kurostream.core.common.dispatcher

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * Shared test [DispatcherProvider] implementation that uses [Dispatchers.Unconfined] for all
 * dispatchers. Keeps unit tests fast and deterministic without requiring Android resources.
 */
actual class TestDispatcherProvider : DispatcherProvider {
    actual override val main: CoroutineDispatcher = Dispatchers.Unconfined
    actual override val io: CoroutineDispatcher = Dispatchers.Unconfined
    actual override val default: CoroutineDispatcher = Dispatchers.Unconfined
    actual override val unconfined: CoroutineDispatcher = Dispatchers.Unconfined
    actual override val computational: CoroutineDispatcher = Dispatchers.Unconfined
    actual override val diskIO: CoroutineDispatcher = Dispatchers.Unconfined
    actual override val networkIO: CoroutineDispatcher = Dispatchers.Unconfined
    actual override val animation: CoroutineDispatcher = Dispatchers.Unconfined
}