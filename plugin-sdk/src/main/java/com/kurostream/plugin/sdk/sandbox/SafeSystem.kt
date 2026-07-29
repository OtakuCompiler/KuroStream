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
 * Safe replacement for java.lang.System that restricts dangerous operations.
 */
class SafeSystem {
    companion object {
        @JvmStatic
        fun currentTimeMillis(): Long = java.lang.System.currentTimeMillis()
        
        @JvmStatic
        fun nanoTime(): Long = java.lang.System.nanoTime()
        
        @JvmStatic
        fun arraycopy(src: Any, srcPos: Int, dest: Any, destPos: Int, length: Int) {
            java.lang.System.arraycopy(src, srcPos, dest, destPos, length)
        }
        
        @JvmStatic
        fun getProperty(key: String): String? {
            // Only allow safe properties
            return when (key) {
                "line.separator", "file.separator", "path.separator", 
                "java.version", "java.vendor", "java.vendor.url", 
                "java.class.version", "os.name", "os.arch", "os.version" -> 
                    java.lang.System.getProperty(key)
                else -> null
            }
        }
        
        @JvmStatic
        fun getenv(name: String): String? = null // Block all environment access
        
        @JvmStatic
        fun exit(status: Int) {
            throw SecurityException("System.exit() is not allowed in extensions")
        }
        
        @JvmStatic
        fun gc() {
            // Silently ignore GC requests
        }
        
        @JvmStatic
        fun runFinalization() {
            // Silently ignore
        }
    }
}