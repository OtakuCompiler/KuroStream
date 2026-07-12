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

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class WebStorage : PlatformStorage {
    override suspend fun writeFile(path: String, data: ByteArray) {
        throw UnsupportedOperationException("WebStorage.writeFile not implemented for webOS/Tizen. Implement in platform-specific module.")
    }
    
    override suspend fun readFile(path: String): ByteArray {
        throw UnsupportedOperationException("WebStorage.readFile not implemented for webOS/Tizen. Implement in platform-specific module.")
    }
    
    override suspend fun deleteFile(path: String) {
        throw UnsupportedOperationException("WebStorage.deleteFile not implemented for webOS/Tizen. Implement in platform-specific module.")
    }
    
    override suspend fun fileExists(path: String): Boolean {
        throw UnsupportedOperationException("WebStorage.fileExists not implemented for webOS/Tizen. Implement in platform-specific module.")
    }
    
    override suspend fun listFiles(path: String): List<String> {
        throw UnsupportedOperationException("WebStorage.listFiles not implemented for webOS/Tizen. Implement in platform-specific module.")
    }
    
    override suspend fun createDirectory(path: String) {
        throw UnsupportedOperationException("WebStorage.createDirectory not implemented for webOS/Tizen. Implement in platform-specific module.")
    }
    
    override suspend fun getFileSize(path: String): Long {
        throw UnsupportedOperationException("WebStorage.getFileSize not implemented for webOS/Tizen. Implement in platform-specific module.")
    }
    
    override fun observeFile(path: String): Flow<ByteArray> = flow { }
    
    override suspend fun putString(key: String, value: String) {
        throw UnsupportedOperationException("WebStorage.putString not implemented for webOS/Tizen. Implement in platform-specific module.")
    }
    
    override suspend fun getString(key: String): String? {
        throw UnsupportedOperationException("WebStorage.getString not implemented for webOS/Tizen. Implement in platform-specific module.")
    }
    
    override suspend fun putBoolean(key: String, value: Boolean) {
        throw UnsupportedOperationException("WebStorage.putBoolean not implemented for webOS/Tizen. Implement in platform-specific module.")
    }
    
    override suspend fun getBoolean(key: String): Boolean {
        throw UnsupportedOperationException("WebStorage.getBoolean not implemented for webOS/Tizen. Implement in platform-specific module.")
    }
    
    override suspend fun putInt(key: String, value: Int) {
        throw UnsupportedOperationException("WebStorage.putInt not implemented for webOS/Tizen. Implement in platform-specific module.")
    }
    
    override suspend fun getInt(key: String): Int {
        throw UnsupportedOperationException("WebStorage.getInt not implemented for webOS/Tizen. Implement in platform-specific module.")
    }
    
    override suspend fun putLong(key: String, value: Long) {
        throw UnsupportedOperationException("WebStorage.putLong not implemented for webOS/Tizen. Implement in platform-specific module.")
    }
    
    override suspend fun getLong(key: String): Long {
        throw UnsupportedOperationException("WebStorage.getLong not implemented for webOS/Tizen. Implement in platform-specific module.")
    }
    
    override suspend fun remove(key: String) {
        throw UnsupportedOperationException("WebStorage.remove not implemented for webOS/Tizen. Implement in platform-specific module.")
    }
    
    override suspend fun clear() {
        throw UnsupportedOperationException("WebStorage.clear not implemented for webOS/Tizen. Implement in platform-specific module.")
    }
}