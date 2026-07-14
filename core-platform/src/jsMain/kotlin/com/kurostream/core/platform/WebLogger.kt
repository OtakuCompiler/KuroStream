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

actual class WebLogger(private val tag: String = "KuroStream") : PlatformLogger {
    private var minLevel = LogLevel.DEBUG
    private val enabledTags = mutableSetOf<String>()
    
    actual fun verbose(tag: String, message: String, throwable: Throwable?) {
        if (minLevel.priority <= LogLevel.VERBOSE.priority && isTagEnabled(tag)) {
            consoleLog("verbose", tag, message, throwable)
        }
    }
    
    actual fun debug(tag: String, message: String, throwable: Throwable?) {
        if (minLevel.priority <= LogLevel.DEBUG.priority && isTagEnabled(tag)) {
            consoleLog("debug", tag, message, throwable)
        }
    }
    
    actual fun info(tag: String, message: String, throwable: Throwable?) {
        if (minLevel.priority <= LogLevel.INFO.priority && isTagEnabled(tag)) {
            consoleLog("info", tag, message, throwable)
        }
    }
    
    actual fun warn(tag: String, message: String, throwable: Throwable?) {
        if (minLevel.priority <= LogLevel.WARN.priority && isTagEnabled(tag)) {
            consoleWarn(tag, message, throwable)
        }
    }
    
    actual fun error(tag: String, message: String, throwable: Throwable?) {
        if (minLevel.priority <= LogLevel.ERROR.priority && isTagEnabled(tag)) {
            consoleError(tag, message, throwable)
        }
    }
    
    actual fun fatal(tag: String, message: String, throwable: Throwable?) {
        if (minLevel.priority <= LogLevel.FATAL.priority && isTagEnabled(tag)) {
            consoleError(tag, message, throwable)
            window.location.reload()
        }
    }
    
    actual fun setMinLevel(level: LogLevel) {
        minLevel = level
    }
    
    actual fun enableTag(tag: String) {
        enabledTags.add(tag)
    }
    
    actual fun disableTag(tag: String) {
        enabledTags.remove(tag)
    }
    
    private fun isTagEnabled(tag: String): Boolean = enabledTags.isEmpty() || enabledTags.contains(tag)
    
    private external fun consoleLog(level: String, tag: String, message: String, throwable: Throwable?)
    private external fun consoleWarn(tag: String, message: String, throwable: Throwable?)
    private external fun consoleError(tag: String, message: String, throwable: Throwable?)
    
    companion object {
        init {
            load()
        }
    }
}

external fun load(): Unit