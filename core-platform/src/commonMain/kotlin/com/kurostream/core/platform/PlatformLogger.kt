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

interface PlatformLogger {
    fun verbose(tag: String, message: String, throwable: Throwable? = null)
    fun debug(tag: String, message: String, throwable: Throwable? = null)
    fun info(tag: String, message: String, throwable: Throwable? = null)
    fun warn(tag: String, message: String, throwable: Throwable? = null)
    fun error(tag: String, message: String, throwable: Throwable? = null)
    fun fatal(tag: String, message: String, throwable: Throwable? = null)
    
    fun setMinLevel(level: LogLevel)
    fun enableTag(tag: String)
    fun disableTag(tag: String)
}

enum class LogLevel(val priority: Int) {
    VERBOSE(0),
    DEBUG(1),
    INFO(2),
    WARN(3),
    ERROR(4),
    FATAL(5),
    NONE(6)
}

class CompositeLogger(private val loggers: List<PlatformLogger>) : PlatformLogger {
    override fun verbose(tag: String, message: String, throwable: Throwable?) = loggers.forEach { it.verbose(tag, message, throwable) }
    override fun debug(tag: String, message: String, throwable: Throwable?) = loggers.forEach { it.debug(tag, message, throwable) }
    override fun info(tag: String, message: String, throwable: Throwable?) = loggers.forEach { it.info(tag, message, throwable) }
    override fun warn(tag: String, message: String, throwable: Throwable?) = loggers.forEach { it.warn(tag, message, throwable) }
    override fun error(tag: String, message: String, throwable: Throwable?) = loggers.forEach { it.error(tag, message, throwable) }
    override fun fatal(tag: String, message: String, throwable: Throwable?) = loggers.forEach { it.fatal(tag, message, throwable) }
    override fun setMinLevel(level: LogLevel) = loggers.forEach { it.setMinLevel(level) }
    override fun enableTag(tag: String) = loggers.forEach { it.enableTag(tag) }
    override fun disableTag(tag: String) = loggers.forEach { it.disableTag(tag) }
}