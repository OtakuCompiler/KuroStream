package com.kurostream.core.platform

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class JvmStorage : PlatformStorage {
    override suspend fun writeFile(path: String, data: ByteArray) {
        throw UnsupportedOperationException("JvmStorage not implemented for JVM.")
    }
    override suspend fun readFile(path: String): ByteArray {
        throw UnsupportedOperationException("JvmStorage not implemented for JVM.")
    }
    override suspend fun deleteFile(path: String) {
        throw UnsupportedOperationException("JvmStorage not implemented for JVM.")
    }
    override suspend fun fileExists(path: String): Boolean = false
    override suspend fun listFiles(path: String): List<String> = emptyList()
    override suspend fun createDirectory(path: String) {
        throw UnsupportedOperationException("JvmStorage not implemented for JVM.")
    }
    override suspend fun getFileSize(path: String): Long = 0L
    override fun observeFile(path: String): Flow<ByteArray> = flow { }
    override suspend fun putString(key: String, value: String) {
        throw UnsupportedOperationException("JvmStorage not implemented for JVM.")
    }
    override suspend fun getString(key: String): String? = null
    override suspend fun putBoolean(key: String, value: Boolean) {
        throw UnsupportedOperationException("JvmStorage not implemented for JVM.")
    }
    override suspend fun getBoolean(key: String): Boolean = false
    override suspend fun putInt(key: String, value: Int) {
        throw UnsupportedOperationException("JvmStorage not implemented for JVM.")
    }
    override suspend fun getInt(key: String): Int = 0
    override suspend fun putLong(key: String, value: Long) {
        throw UnsupportedOperationException("JvmStorage not implemented for JVM.")
    }
    override suspend fun getLong(key: String): Long = 0L
    override suspend fun remove(key: String) {
        throw UnsupportedOperationException("JvmStorage not implemented for JVM.")
    }
    override suspend fun clear() {
        throw UnsupportedOperationException("JvmStorage not implemented for JVM.")
    }
}
