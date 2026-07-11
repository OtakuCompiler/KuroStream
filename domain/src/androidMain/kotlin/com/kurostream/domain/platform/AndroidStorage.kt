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

package com.kurostream.domain.platform

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.StandardOpenOption

class AndroidStorage(
    private val context: Context,
    private val scope: CoroutineScope
) : PlatformStorage {
    
    override suspend fun writeFile(path: String, data: ByteArray) = withContext(Dispatchers.IO) {
        File(path).parentFile?.mkdirs()
        FileOutputStream(path).use { it.write(data) }
    }
    
    override suspend fun readFile(path: String): ByteArray = withContext(Dispatchers.IO) {
        File(path).readBytes()
    }
    
    override suspend fun deleteFile(path: String) = withContext(Dispatchers.IO) {
        File(path).delete()
    }
    
    override suspend fun fileExists(path: String): Boolean = withContext(Dispatchers.IO) {
        File(path).exists()
    }
    
    override suspend fun listFiles(path: String): List<String> = withContext(Dispatchers.IO) {
        File(path).listFiles()?.map { it.absolutePath } ?: emptyList()
    }
    
    override suspend fun createDirectory(path: String) = withContext(Dispatchers.IO) {
        File(path).mkdirs()
    }
    
    override suspend fun getFileSize(path: String): Long = withContext(Dispatchers.IO) {
        File(path).length()
    }
    
    override fun observeFile(path: String) = callbackFlow {
        val file = File(path)
        var lastModified = file.lastModified()
        val channel = Channel<ByteArray>()
        
        val watcher = Thread {
            while (isActive) {
                Thread.sleep(1000)
                if (file.exists() && file.lastModified() != lastModified) {
                    lastModified = file.lastModified()
                    channel.trySend(file.readBytes())
                }
            }
        }.apply { start() }
        
        try {
            collectWhile { bytes ->
                trySend(bytes)
            }
        } finally {
            watcher.interrupt()
            channel.close()
        }
    }.asStateFlow()
    
    override suspend fun putString(key: String, value: String) = withContext(Dispatchers.IO) {
        context.getSharedPreferences("kurostream_prefs", Context.MODE_PRIVATE)
            .edit()
            .putString(key, value)
            .apply()
    }
    
    override suspend fun getString(key: String): String? = withContext(Dispatchers.IO) {
        context.getSharedPreferences("kurostream_prefs", Context.MODE_PRIVATE)
            .getString(key, null)
    }
    
    override suspend fun putBoolean(key: String, value: Boolean) = withContext(Dispatchers.IO) {
        context.getSharedPreferences("kurostream_prefs", Context.MODE_PRIVATE)
            .edit()
            .putBoolean(key, value)
            .apply()
    }
    
    override suspend fun getBoolean(key: String): Boolean = withContext(Dispatchers.IO) {
        context.getSharedPreferences("kurostream_prefs", Context.MODE_PRIVATE)
            .getBoolean(key, false)
    }
    
    override suspend fun putInt(key: String, value: Int) = withContext(Dispatchers.IO) {
        context.getSharedPreferences("kurostream_prefs", Context.MODE_PRIVATE)
            .edit()
            .putInt(key, value)
            .apply()
    }
    
    override suspend fun getInt(key: String): Int = withContext(Dispatchers.IO) {
        context.getSharedPreferences("kurostream_prefs", Context.MODE_PRIVATE)
            .getInt(key, 0)
    }
    
    override suspend fun putLong(key: String, value: Long) = withContext(Dispatchers.IO) {
        context.getSharedPreferences("kurostream_prefs", Context.MODE_PRIVATE)
            .edit()
            .putLong(key, value)
            .apply()
    }
    
    override suspend fun getLong(key: String): Long = withContext(Dispatchers.IO) {
        context.getSharedPreferences("kurostream_prefs", Context.MODE_PRIVATE)
            .getLong(key, 0L)
    }
    
    override suspend fun remove(key: String) = withContext(Dispatchers.IO) {
        context.getSharedPreferences("kurostream_prefs", Context.MODE_PRIVATE)
            .edit()
            .remove(key)
            .apply()
    }
    
    override suspend fun clear() = withContext(Dispatchers.IO) {
        context.getSharedPreferences("kurostream_prefs", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }
}