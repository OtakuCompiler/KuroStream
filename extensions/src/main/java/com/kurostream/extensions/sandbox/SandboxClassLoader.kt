// This file is part of KuroStream.
//
// SandboxClassLoader — security-hardened ClassLoader for extension APKs.
//
// Delegates to DexClassLoader for actual class resolution but enforces
// package-level blocklists and dangerous-class restrictions.
//
// Key design (Java ClassLoader contract):
//   loadClass(name) — public entry point, checks cache → parent → findClass()
//   findClass(name) — subclass hook, does the actual loading
//
// The previous implementation overrode loadClass() directly but never called
// findClass() / DexClassLoader, so NO extension classes could load.
// This version overrides findClass() so the standard parent-first delegation
// (framework classes) still works, and DexClassLoader resolves extension
// classes on cache miss.
//
// SPDX-License-Identifier: GPL-3.0-only
package com.kurostream.extensions.sandbox

import dalvik.system.DexClassLoader
import java.io.File

/**
 * Security-hardened ClassLoader for extension APKs.
 *
 * @param dexPath             absolute path to the extension APK
 * @param optimizedDirectory  where optimized dex files are written
 * @param librarySearchPath   native library search path (null = none)
 * @param parent              parent ClassLoader (app classloader)
 * @param allowedPackages     packages extension classes may live in
 * @param blockedPackages     packages that are unconditionally blocked
 */
class SandboxClassLoader(
    dexPath: String,
    optimizedDirectory: File?,
    librarySearchPath: String?,
    parent: ClassLoader?,
    private val allowedPackages: Set<String>,
    private val blockedPackages: Set<String>
) : ClassLoader(parent) {

    private val dexLoader: DexClassLoader = DexClassLoader(
        dexPath,
        optimizedDirectory?.absolutePath,
        librarySearchPath,
        parent
    )

    companion object {
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
            "dalvik.system.PathClassLoader",
        )

        private val SAFE_REPLACEMENTS = mapOf(
            "java.lang.System" to "com.kurostream.plugin.sdk.sandbox.SafeSystem",
            "java.lang.Runtime" to "com.kurostream.plugin.sdk.sandbox.SafeRuntime",
        )
    }

    override fun findClass(name: String): Class<*> {
        if (name == null) {
            throw ClassNotFoundException("Null class name")
        }

        SAFE_REPLACEMENTS[name]?.let { safeName ->
            return parent.loadClass(safeName)
        }

        if (DANGEROUS_CLASSES.contains(name)) {
            throw SecurityException("Dangerous class blocked: $name")
        }

        for (blocked in blockedPackages) {
            if (name.startsWith(blocked)) {
                throw SecurityException("Package blocked: $name")
            }
        }

        val isFramework = name.startsWith("android.") ||
                name.startsWith("kotlin.") ||
                name.startsWith("kotlinx.") ||
                name.startsWith("java.") ||
                name.startsWith("javax.")
        val isAllowed = allowedPackages.any { name.startsWith(it) }

        if (!isFramework && !isAllowed) {
            throw SecurityException("Class not in allowed package list: $name")
        }

        return try {
            dexLoader.loadClass(name)
        } catch (e: ClassNotFoundException) {
            throw ClassNotFoundException("Extension class not found: $name", e)
        }
    }
}
