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

import dalvik.system.DexClassLoader
import java.io.File

/**
 * Restricted ClassLoader for extension sandbox.
 * Blocks dangerous classes and packages.
 */
class SandboxClassLoader(
    private val parent: ClassLoader?,
    private val allowedPackages: Set<String>,
    private val blockedPackages: Set<String>
) : ClassLoader(parent) {

    override fun loadClass(name: String?): Class<*> {
        // Check blocked packages first
        if (name != null) {
            for (blocked in blockedPackages) {
                if (name.startsWith(blocked)) {
                    throw SecurityException("Access to class $name is blocked in sandbox")
                }
            }
        }

        // Check allowed packages
        val isAllowed = name == null || allowedPackages.any { name.startsWith(it) }
        if (!isAllowed) {
            throw SecurityException("Class $name is not in the allowed package list")
        }

        return super.loadClass(name)
    }

    override fun findClass(name: String?): Class<*> {
        return super.findClass(name)
    }
}
