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

class WebNetwork : PlatformNetwork {
    override suspend fun get(url: String, headers: Map<String, String>): HttpResponse {
        throw UnsupportedOperationException("WebNetwork.get not implemented for webOS/Tizen. Implement in platform-specific module.")
    }
    
    override suspend fun post(url: String, body: String?, headers: Map<String, String>): HttpResponse {
        throw UnsupportedOperationException("WebNetwork.post not implemented for webOS/Tizen. Implement in platform-specific module.")
    }
    
    override suspend fun put(url: String, body: String?, headers: Map<String, String>): HttpResponse {
        throw UnsupportedOperationException("WebNetwork.put not implemented for webOS/Tizen. Implement in platform-specific module.")
    }
    
    override suspend fun delete(url: String, headers: Map<String, String>): HttpResponse {
        throw UnsupportedOperationException("WebNetwork.delete not implemented for webOS/Tizen. Implement in platform-specific module.")
    }
    
    override fun webSocket(url: String): Flow<WebSocketMessage> = flow { }
    
    override fun webSocketWithHeaders(url: String, headers: Map<String, String>): Flow<WebSocketMessage> = flow { }
    
    override fun download(url: String, destinationPath: String, progress: Flow<Float>): Flow<DownloadResult> = flow { }
    
    override suspend fun ping(host: String): Boolean {
        throw UnsupportedOperationException("WebNetwork.ping not implemented for webOS/Tizen. Implement in platform-specific module.")
    }
    
    override suspend fun getConnectionQuality(): ConnectionQuality {
        throw UnsupportedOperationException("WebNetwork.getConnectionQuality not implemented for webOS/Tizen. Implement in platform-specific module.")
    }
}
