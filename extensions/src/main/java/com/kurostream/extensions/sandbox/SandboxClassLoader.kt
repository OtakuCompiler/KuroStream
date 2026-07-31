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

package com.kurostream.extensions.sandbox

import dalvik.system.DexClassLoader
import java.io.File

/**
 * Restricted ClassLoader for extension sandbox.
 * Blocks dangerous classes and packages, and prevents reflection attacks.
 *
 * WARNING: This is a blocklist-based isolation pattern. It is known to be
 * bypassable via indirect reflection, parent-loader lookup, and JNI calls.
 * Do NOT load untrusted third-party code through this classloader until the
 * sandbox is properly hardened (out of scope for this pass).
 */
class SandboxClassLoader(
    private val parent: ClassLoader?,
    private val allowedPackages: Set<String>,
    private val blockedPackages: Set<String>
) : ClassLoader(parent) {

    companion object {
        // Additional dangerous classes that should be blocked
        private val DANGEROUS_CLASSES = setOf(
            "java.lang.Class",
            "java.lang.reflect.Method",
            "java.lang.reflect.Field",
            "java.lang.reflect.Constructor",
            "java.lang.invoke.MethodHandles",
            "java.lang.invoke.MethodHandle",
            "java.lang.invoke.VarHandle",
            "java.lang.System",
            "java.lang.Runtime",
            "java.lang.ProcessBuilder",
            "java.lang.Thread",
            "java.lang.ThreadGroup",
            "java.lang.SecurityManager",
            "dalvik.system.BaseDexClassLoader",
            "dalvik.system.DexClassLoader",
            "dalvik.system.PathClassLoader"
        )
        
        // Classes that should be replaced with safe alternatives
        private val SAFE_REPLACEMENTS = mapOf(
            "java.lang.System" to "com.kurostream.plugin.sdk.sandbox.SafeSystem",
            "java.lang.Runtime" to "com.kurostream.plugin.sdk.sandbox.SafeRuntime"
        )
    }

    override fun loadClass(name: String?): Class<*> {
        // Check for null
        if (name == null) {
            throw ClassNotFoundException("Null class name")
        }
        
        // Check dangerous classes
        if (DANGEROUS_CLASSES.contains(name)) {
            throw SecurityException("Access to dangerous class $name is blocked in sandbox")
        }
        
        // Check for safe replacements
        SAFE_REPLACEMENTS[name]?.let { safeClassName ->
            return super.loadClass(safeClassName)
        }
        
        // Check blocked packages first
        for (blocked in blockedPackages) {
            if (name.startsWith(blocked)) {
                throw SecurityException("Access to class $name is blocked in sandbox")
            }
        }

        // Check allowed packages
        val isAllowed = allowedPackages.any { name.startsWith(it) }
        if (!isAllowed) {
            throw SecurityException("Class $name is not in the allowed package list")
        }

        return super.loadClass(name)
    }

    override fun findClass(name: String?): Class<*> {
        // Prevent loading classes from arbitrary locations
        throw SecurityException("Custom class loading is not allowed in sandbox")
    }
}
