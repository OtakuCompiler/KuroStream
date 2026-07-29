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

interface PlatformUI {
    fun openUrl(url: String)
    fun shareText(text: String, subject: String?)
    fun showToast(message: String)
    fun showError(message: String)
    fun vibrate(pattern: LongArray? = null)
    
    fun requestFocus(focusable: Focusable)
    fun clearFocus()
    
    fun setDisplayRefreshRate(rate: Float)
    fun getDisplayRefreshRate(): Float
    
    fun setImmersiveMode(enabled: Boolean)
    fun isImmersiveMode(): Boolean
}

interface Focusable {
    val focusId: String
    fun onFocusGained()
    fun onFocusLost()
}