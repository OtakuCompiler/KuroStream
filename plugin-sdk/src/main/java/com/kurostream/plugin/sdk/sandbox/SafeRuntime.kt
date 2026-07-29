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

package com.kurostream.plugin.sdk.sandbox

/**
 * Safe replacement for java.lang.Runtime that restricts dangerous operations.
 */
class SafeRuntime private constructor() {
    companion object {
        @JvmStatic
        fun getRuntime(): SafeRuntime = SafeRuntime()
    }

    fun availableProcessors(): Int = 1 // Limit to 1 processor
    fun freeMemory(): Long = 1024 * 1024 // Report 1MB free
    fun totalMemory(): Long = 16 * 1024 * 1024 // Report 16MB total
    fun maxMemory(): Long = 16 * 1024 * 1024 // Report 16MB max

    fun exec(command: String): Process {
        throw SecurityException("Runtime.exec() is not allowed in extensions")
    }
    fun exec(command: String, envp: Array<String>): Process {
        throw SecurityException("Runtime.exec() is not allowed in extensions")
    }
    fun exec(command: String, envp: Array<String>, dir: java.io.File): Process {
        throw SecurityException("Runtime.exec() is not allowed in extensions")
    }
    fun exec(cmdarray: Array<String>): Process {
        throw SecurityException("Runtime.exec() is not allowed in extensions")
    }
    fun exec(cmdarray: Array<String>, envp: Array<String>): Process {
        throw SecurityException("Runtime.exec() is not allowed in extensions")
    }
    fun exec(cmdarray: Array<String>, envp: Array<String>, dir: java.io.File): Process {
        throw SecurityException("Runtime.exec() is not allowed in extensions")
    }

    fun gc() { /* Silently ignore */ }
    fun runFinalization() { /* Silently ignore */ }
    fun load(filename: String) {
        throw SecurityException("Runtime.load() is not allowed in extensions")
    }
    fun loadLibrary(libname: String) {
        throw SecurityException("Runtime.loadLibrary() is not allowed in extensions")
    }
    fun traceInstructions(on: Boolean) { /* Silently ignore */ }
    fun traceMethodCalls(on: Boolean) { /* Silently ignore */ }
}