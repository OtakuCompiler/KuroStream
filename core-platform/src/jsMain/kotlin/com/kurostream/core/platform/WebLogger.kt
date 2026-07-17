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

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.js.Date
import kotlin.js.unsafeCast
import kotlin.js.json

@JsModule("console")
@JsNonModule
external fun consoleLog(level: String, tag: String, message: String, throwable: Any?)

@JsModule("console")
@JsNonModule
external fun consoleWarn(tag: String, message: String, throwable: Any?)

@JsModule("console")
@JsNonModule
external fun consoleError(tag: String, message: String, throwable: Any?)

external fun reloadPage()

class WebLogger : PlatformLogger {
    private val enabledTags = mutableSetOf<String>()
    private val minLevel = LogLevel.INFO

    private val _logs = MutableSharedFlow<LogEntry>(replay = 1000)
    override val logs = _logs.asStateFlow()

    init {
        // Initialization
    }

    override fun verbose(tag: String, message: String, throwable: Throwable?) {
        log(LogLevel.VERBOSE, tag, message, throwable)
    }

    override fun debug(tag: String, message: String, throwable: Throwable?) {
        log(LogLevel.DEBUG, tag, message, throwable)
    }

    override fun info(tag: String, message: String, throwable: Throwable?) {
        log(LogLevel.INFO, tag, message, throwable)
    }

    override fun warn(tag: String, message: String, throwable: Throwable?) {
        log(LogLevel.WARN, tag, message, throwable)
    }

    override fun error(tag: String, message: String, throwable: Throwable?) {
        log(LogLevel.ERROR, tag, message, throwable)
    }

    override fun fatal(tag: String, message: String, throwable: Throwable?) {
        log(LogLevel.FATAL, tag, message, throwable)
    }

    private fun log(level: LogLevel, tag: String, message: String, throwable: Throwable?) {
        if (level.ordinal >= minLevel.ordinal && isTagEnabled(tag)) {
            val entry = LogEntry(level, tag, message, throwable)
            _logs.tryEmit(entry)

            when (level) {
                LogLevel.DEBUG -> consoleLog("debug", tag, message, throwable)
                LogLevel.VERBOSE, LogLevel.INFO -> consoleLog("log", tag, message, throwable)
                LogLevel.WARN -> consoleWarn(tag, message, throwable)
                LogLevel.ERROR, LogLevel.FATAL -> consoleError(tag, message, throwable)
                else -> consoleLog("log", tag, message, throwable)
            }
        }
    }

    override fun setMinLevel(level: LogLevel) {
        minLevel = level
    }

    override fun enableTag(tag: String) {
        enabledTags.add(tag)
    }

    override fun disableTag(tag: String) {
        enabledTags.remove(tag)
    }

    private fun isTagEnabled(tag: String): Boolean = enabledTags.isEmpty() || enabledTags.contains(tag)

    data class LogEntry(
        val level: LogLevel,
        val tag: String,
        val message: String,
        val throwable: Throwable?,
        val timestamp: Long = Date.now()
    )
}