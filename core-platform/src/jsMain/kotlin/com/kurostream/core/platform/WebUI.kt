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

class WebUI : PlatformUI {
    override fun openUrl(url: String) {
        throw UnsupportedOperationException("WebUI.openUrl not implemented for webOS/Tizen. Implement in platform-specific module.")
    }
    
    override fun shareText(text: String, subject: String?) {
        throw UnsupportedOperationException("WebUI.shareText not implemented for webOS/Tizen. Implement in platform-specific module.")
    }
    
    override fun showToast(message: String) {
        throw UnsupportedOperationException("WebUI.showToast not implemented for webOS/Tizen. Implement in platform-specific module.")
    }
    
    override fun showError(message: String) {
        throw UnsupportedOperationException("WebUI.showError not implemented for webOS/Tizen. Implement in platform-specific module.")
    }
    
    override fun vibrate(pattern: LongArray?) {
        throw UnsupportedOperationException("WebUI.vibrate not implemented for webOS/Tizen. Implement in platform-specific module.")
    }
    
    override fun requestFocus(focusable: Focusable) {
        throw UnsupportedOperationException("WebUI.requestFocus not implemented for webOS/Tizen. Implement in platform-specific module.")
    }
    
    override fun clearFocus() {
        throw UnsupportedOperationException("WebUI.clearFocus not implemented for webOS/Tizen. Implement in platform-specific module.")
    }
    
    override fun setDisplayRefreshRate(rate: Float) {
        throw UnsupportedOperationException("WebUI.setDisplayRefreshRate not implemented for webOS/Tizen. Implement in platform-specific module.")
    }
    
    override fun getDisplayRefreshRate(): Float {
        throw UnsupportedOperationException("WebUI.getDisplayRefreshRate not implemented for webOS/Tizen. Implement in platform-specific module.")
    }
    
    override fun setImmersiveMode(enabled: Boolean) {
        throw UnsupportedOperationException("WebUI.setImmersiveMode not implemented for webOS/Tizen. Implement in platform-specific module.")
    }
    
    override fun isImmersiveMode(): Boolean {
        throw UnsupportedOperationException("WebUI.isImmersiveMode not implemented for webOS/Tizen. Implement in platform-specific module.")
    }
}
