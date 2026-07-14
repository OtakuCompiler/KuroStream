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

actual class PlatformContext {
    actual fun getPackageName(): String = "com.kurostream.web"
    
    actual fun getVersionName(): String = "1.0.0"
    
    actual fun getVersionCode(): Long = 1
    
    actual fun getApplicationName(): String = "KuroStream Web"
    
    actual fun getCacheDir(): String = "/tmp/kurostream_cache"
    
    actual fun getFilesDir(): String = "/tmp/kurostream_files"
    
    actual fun getExternalFilesDir(type: String?): String? = null
    
    actual fun isDebuggable(): Boolean = false
    
    actual fun getSystemService(name: String): Any? = null
}
