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

package com.kurostream.players.advanced.extensions.sandbox

import android.app.Service
import android.content.Intent
import android.os.*
import android.os.Process
import android.util.Log
import kotlinx.coroutines.*
import java.io.File
import java.io.RandomAccessFile
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * Extension sandbox running in an isolated process.
 * Uses Binder IPC for communication with the main app.
 * Enforces CPU, memory, and execution time limits.
 */
class IsolatedExtensionService : Service() {

    companion object {
        const val TAG = "ExtensionSandbox"
        const val EXTRA_EXTENSION_ID = "extension_id"
        const val EXTRA_EXTENSION_PACKAGE = "extension_package"
        const val EXTRA_SCRIPT_PATH = "script_path"
        const val EXTRA_MEMORY_LIMIT_MB = "memory_limit_mb"
        const val EXTRA_CPU_TIME_LIMIT_MS = "cpu_time_limit_ms"

        const val DEFAULT_MEMORY_LIMIT_MB = 128
        const val DEFAULT_CPU_TIME_LIMIT_MS = 5000
        const val MAX_CONCURRENT_EXTENSIONS = 4
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val isRunning = AtomicBoolean(false)
    private val activeExtensions = ConcurrentHashMap<String, ExtensionInstance>()
    private val extensionCounter = AtomicInteger(0)

    // Resource limits
    private val memoryLimitBytes = AtomicLong(DEFAULT_MEMORY_LIMIT_MB * 1024L * 1024L)
    private val cpuTimeLimitMs = AtomicLong(DEFAULT_CPU_TIME_LIMIT_MS.toLong())

    private lateinit var binder: ExtensionSandboxBinder

    data class ExtensionInstance(
        val id: String,
        val packageName: String,
        val processId: Int,
        val startTime: Long,
        val memoryLimit: Long,
        val cpuTimeLimit: Long,
        val scope: CoroutineScope,
        val isActive: AtomicBoolean = AtomicBoolean(true)
    ) {
        val currentMemoryUsage = AtomicLong(0)
        val currentCpuTimeMs = AtomicLong(0)
    }

    data class ExtensionResult(
        val success: Boolean,
        val output: String,
        val error: String? = null,
        val memoryUsed: Long = 0,
        val cpuTimeMs: Long = 0
    )

    override fun onCreate() {
        super.onCreate()
        isRunning.set(true)
        binder = ExtensionSandboxBinder()

        // Start resource monitoring
        serviceScope.launch { monitorResources() }
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            val extensionId = it.getStringExtra(EXTRA_EXTENSION_ID) ?: return START_NOT_STICKY
            val packageName = it.getStringExtra(EXTRA_EXTENSION_PACKAGE) ?: return START_NOT_STICKY
            val scriptPath = it.getStringExtra(EXTRA_SCRIPT_PATH) ?: return START_NOT_STICKY
            val memoryLimit = it.getIntExtra(EXTRA_MEMORY_LIMIT_MB, DEFAULT_MEMORY_LIMIT_MB)
            val cpuLimit = it.getIntExtra(EXTRA_CPU_TIME_LIMIT_MS, DEFAULT_CPU_TIME_LIMIT_MS)

            serviceScope.launch {
                launchExtension(extensionId, packageName, scriptPath, memoryLimit, cpuLimit)
            }
        }
        return START_NOT_STICKY
    }

    private suspend fun launchExtension(
        extensionId: String,
        packageName: String,
        scriptPath: String,
        memoryLimitMb: Int,
        cpuTimeLimitMs: Int
    ) {
        if (activeExtensions.size >= MAX_CONCURRENT_EXTENSIONS) {
            Log.w(TAG, "Max concurrent extensions reached, cannot launch $extensionId")
            return
        }

        val memoryLimitBytes = memoryLimitMb * 1024L * 1024L
        val instance = ExtensionInstance(
            id = extensionId,
            packageName = packageName,
            processId = Process.myPid(),
            startTime = System.currentTimeMillis(),
            memoryLimit = memoryLimitBytes,
            cpuTimeLimit = cpuTimeLimitMs.toLong(),
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        )

        activeExtensions[extensionId] = instance

        try {
            // Set memory limits using cgroup (requires root on most devices)
            // Fallback: Use Runtime.maxMemory() and monitor
            enforceMemoryLimit(instance)

            // Execute extension in sandboxed context
            val result = executeInSandbox(scriptPath, instance)

            // Report result back to main process
            reportResult(extensionId, result)

        } catch (e: SecurityException) {
            Log.e(TAG, "Extension $extensionId violated sandbox policy", e)
            killExtension(extensionId, "Security violation")
        } catch (e: OutOfMemoryError) {
            Log.e(TAG, "Extension $extensionId exceeded memory limit", e)
            killExtension(extensionId, "Memory limit exceeded")
        } catch (e: Exception) {
            Log.e(TAG, "Extension $extensionId failed", e)
            reportResult(extensionId, ExtensionResult(
                success = false,
                output = "",
                error = e.message
            ))
        } finally {
            activeExtensions.remove(extensionId)
            instance.scope.cancel()
        }
    }

    private suspend fun executeInSandbox(scriptPath: String, instance: ExtensionInstance): ExtensionResult {
        return withContext(Dispatchers.Default) {
            val startTime = System.currentTimeMillis()
            val runtime = Runtime.getRuntime()
            val initialMemory = runtime.totalMemory() - runtime.freeMemory()

            try {
                // Create restricted classloader
                val sandboxClassloader = SandboxClassLoader(
                    parent = javaClass.classLoader,
                    allowedPackages = setOf(
                        "com.kurostream.extension.api",
                        "kotlin",
                        "kotlinx.coroutines",
                        "java.lang",
                        "java.util",
                        "java.io",
                        "java.net"
                    ),
                    blockedPackages = setOf(
                        "android.os.Process",
                        "java.lang.reflect",
                        "java.lang.invoke",
                        "sun.misc",
                        "dalvik.system"
                    )
                )

                // Load and execute extension script
                val scriptFile = File(scriptPath)
                val scriptContent = scriptFile.readText()

                // Parse and validate script (basic static analysis)
                val validationResult = validateScript(scriptContent)
                if (!validationResult.isValid) {
                    return@withContext ExtensionResult(
                        success = false,
                        output = "",
                        error = "Script validation failed: ${validationResult.reason}"
                    )
                }

                // Execute with timeout
                val executionResult = withTimeoutOrNull(instance.cpuTimeLimit) {
                    executeScript(scriptContent, sandboxClassloader, instance)
                }

                val endTime = System.currentTimeMillis()
                val finalMemory = runtime.totalMemory() - runtime.freeMemory()
                val memoryUsed = finalMemory - initialMemory

                if (executionResult == null) {
                    return@withContext ExtensionResult(
                        success = false,
                        output = "",
                        error = "Execution timed out after ${instance.cpuTimeLimit}ms",
                        memoryUsed = memoryUsed,
                        cpuTimeMs = endTime - startTime
                    )
                }

                ExtensionResult(
                    success = true,
                    output = executionResult,
                    memoryUsed = memoryUsed,
                    cpuTimeMs = endTime - startTime
                )

            } catch (e: Exception) {
                ExtensionResult(
                    success = false,
                    output = "",
                    error = e.message,
                    cpuTimeMs = System.currentTimeMillis() - startTime
                )
            }
        }
    }

    private fun executeScript(script: String, classloader: ClassLoader, instance: ExtensionInstance): String {
        // This is a simplified execution model
        // Production would use a proper JS engine (QuickJS/V8) or Kotlin Scripting
        val engine = QuickJSEngine(classloader)
        return engine.evaluate(script, instance.memoryLimit)
    }

    private fun validateScript(script: String): ScriptValidationResult {
        val blockedPatterns = listOf(
            "Runtime\.getRuntime\(\)",
            "ProcessBuilder",
            "System\.exit",
            "Class\.forName",
            "java\.io\.File\.",
            "android\.os\.Process",
            "setSecurityManager",
            "defineClass"
        )

        for (pattern in blockedPatterns) {
            if (Regex(pattern).containsMatchIn(script)) {
                return ScriptValidationResult(false, "Blocked pattern: $pattern")
            }
        }

        return ScriptValidationResult(true, null)
    }

    data class ScriptValidationResult(val isValid: Boolean, val reason: String?)

    private fun enforceMemoryLimit(instance: ExtensionInstance) {
        // On rooted devices, use cgroups
        // On standard devices, monitor and kill if exceeded
        serviceScope.launch {
            while (instance.isActive.get()) {
                val runtime = Runtime.getRuntime()
                val used = runtime.totalMemory() - runtime.freeMemory()
                instance.currentMemoryUsage.set(used)

                if (used > instance.memoryLimit) {
                    Log.w(TAG, "Extension ${instance.id} memory limit exceeded: $used > ${instance.memoryLimit}")
                    killExtension(instance.id, "Memory limit exceeded")
                    break
                }
                delay(100)
            }
        }
    }

    private suspend fun monitorResources() {
        while (isActive && isRunning.get()) {
            activeExtensions.values.forEach { instance ->
                val elapsed = System.currentTimeMillis() - instance.startTime
                if (elapsed > instance.cpuTimeLimit) {
                    Log.w(TAG, "Extension ${instance.id} CPU time limit exceeded")
                    killExtension(instance.id, "CPU time limit exceeded")
                }
            }
            delay(500)
        }
    }

    private fun killExtension(extensionId: String, reason: String) {
        activeExtensions[extensionId]?.let { instance ->
            instance.isActive.set(false)
            instance.scope.cancel()
            Log.i(TAG, "Killed extension $extensionId: $reason")
        }
    }

    private fun reportResult(extensionId: String, result: ExtensionResult) {
        // Send result back to main process via broadcast or callback
        val intent = Intent("com.kurostream.EXTENSION_RESULT").apply {
            putExtra("extension_id", extensionId)
            putExtra("success", result.success)
            putExtra("output", result.output)
            result.error?.let { putExtra("error", it) }
            putExtra("memory_used", result.memoryUsed)
            putExtra("cpu_time_ms", result.cpuTimeMs)
        }
        sendBroadcast(intent)
    }

    override fun onDestroy() {
        isRunning.set(false)
        activeExtensions.values.forEach { it.scope.cancel() }
        activeExtensions.clear()
        serviceScope.cancel()
        super.onDestroy()
    }

    /**
     * Binder interface for IPC with main process.
     */
    inner class ExtensionSandboxBinder : Binder() {
        fun getService(): IsolatedExtensionService = this@IsolatedExtensionService

        fun executeExtension(extensionId: String, script: String, callback: IExtensionCallback) {
            serviceScope.launch {
                val instance = activeExtensions[extensionId]
                if (instance == null) {
                    callback.onError(extensionId, "Extension not found")
                    return@launch
                }

                val result = executeInSandbox(script, instance)
                if (result.success) {
                    callback.onSuccess(extensionId, result.output)
                } else {
                    callback.onError(extensionId, result.error ?: "Unknown error")
                }
            }
        }

        fun getExtensionStatus(extensionId: String): ExtensionStatus {
            val instance = activeExtensions[extensionId]
            return if (instance != null) {
                ExtensionStatus(
                    isRunning = instance.isActive.get(),
                    memoryUsed = instance.currentMemoryUsage.get(),
                    memoryLimit = instance.memoryLimit,
                    cpuTimeMs = instance.currentCpuTimeMs.get(),
                    cpuTimeLimit = instance.cpuTimeLimit
                )
            } else {
                ExtensionStatus(isRunning = false)
            }
        }
    }

    data class ExtensionStatus(
        val isRunning: Boolean,
        val memoryUsed: Long = 0,
        val memoryLimit: Long = 0,
        val cpuTimeMs: Long = 0,
        val cpuTimeLimit: Long = 0
    )
}

/**
 * AIDL callback interface for extension results.
 */
interface IExtensionCallback {
    fun onSuccess(extensionId: String, output: String)
    fun onError(extensionId: String, error: String)
    fun onProgress(extensionId: String, progress: Float)
}
