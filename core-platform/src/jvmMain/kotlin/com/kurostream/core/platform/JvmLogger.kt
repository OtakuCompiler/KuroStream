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

import java.util.logging.Level

class JvmLogger(private val tag: String = "KuroStream") : PlatformLogger {
    private var minLevel = LogLevel.DEBUG
    private val enabledTags = mutableSetOf<String>()
    private val logger = java.util.logging.Logger.getLogger(tag)
    
    override fun verbose(tag: String, message: String, throwable: Throwable?) {
        if (minLevel.priority <= LogLevel.VERBOSE.priority && isTagEnabled(tag)) {
            logger.log(Level.FINE, message, throwable)
        }
    }
    
    override fun debug(tag: String, message: String, throwable: Throwable?) {
        if (minLevel.priority <= LogLevel.DEBUG.priority && isTagEnabled(tag)) {
            logger.log(Level.FINER, message, throwable)
        }
    }
    
    override fun info(tag: String, message: String, throwable: Throwable?) {
        if (minLevel.priority <= LogLevel.INFO.priority && isTagEnabled(tag)) {
            logger.log(Level.INFO, message, throwable)
        }
    }
    
    override fun warn(tag: String, message: String, throwable: Throwable?) {
        if (minLevel.priority <= LogLevel.WARN.priority && isTagEnabled(tag)) {
            logger.log(Level.WARNING, message, throwable)
        }
    }
    
    override fun error(tag: String, message: String, throwable: Throwable?) {
        if (minLevel.priority <= LogLevel.ERROR.priority && isTagEnabled(tag)) {
            logger.log(Level.SEVERE, message, throwable)
        }
    }
    
    override fun fatal(tag: String, message: String, throwable: Throwable?) {
        if (minLevel.priority <= LogLevel.FATAL.priority && isTagEnabled(tag)) {
            logger.log(Level.SEVERE, message, throwable)
            System.exit(1)
        }
    }
    
    override fun setMinLevel(level: LogLevel) {
        minLevel = level
        val javaLevel = when (level) {
            LogLevel.VERBOSE -> Level.FINEST
            LogLevel.DEBUG -> Level.FINE
            LogLevel.INFO -> Level.INFO
            LogLevel.WARN -> Level.WARNING
            LogLevel.ERROR -> Level.SEVERE
            LogLevel.FATAL -> Level.SEVERE
            LogLevel.NONE -> Level.OFF
        }
        logger.level = javaLevel
    }
    
    override fun enableTag(tag: String) {
        enabledTags.add(tag)
    }
    
    override fun disableTag(tag: String) {
        enabledTags.remove(tag)
    }
    
    private fun isTagEnabled(tag: String): Boolean = enabledTags.isEmpty() || enabledTags.contains(tag)
}