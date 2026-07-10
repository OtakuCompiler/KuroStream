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

package com.kurostream.data.anistream.subtitle

import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SubtitleCache @Inject constructor(
    cacheDir: File
) {
    private val subtitleDir = File(cacheDir, "subtitles").apply { mkdirs() }

    fun getCachedSubtitle(subtitleId: String): File? {
        val cachedFile = File(subtitleDir, "$subtitleId.srt")
        return if (cachedFile.exists()) cachedFile else null
    }

    fun cacheSubtitle(subtitleId: String, subtitleFile: File): File {
        val cachedFile = File(subtitleDir, "$subtitleId.srt")
        subtitleFile.copyTo(cachedFile, overwrite = true)
        return cachedFile
    }

    fun clearCache() {
        subtitleDir.listFiles()?.forEach { it.delete() }
    }

    fun getCacheSize(): Long {
        return subtitleDir.listFiles()?.sumOf { it.length() } ?: 0
    }
}
