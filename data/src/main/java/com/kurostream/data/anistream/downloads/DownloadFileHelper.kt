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

package com.kurostream.data.anistream.downloads

import android.content.Context
import java.io.File

object DownloadFileHelper {

    fun getDownloadDirectory(context: Context): File {
        val dir = File(context.getExternalFilesDir(null), "downloads")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getDownloadFile(context: Context, downloadId: String): File {
        return File(getDownloadDirectory(context), "$downloadId.mp4")
    }

    fun getTempFile(context: Context, downloadId: String): File {
        return File(getDownloadDirectory(context), "$downloadId.tmp")
    }

    fun getAvailableSpace(context: Context): Long {
        val dir = getDownloadDirectory(context)
        return dir.usableSpace
    }

    fun cleanupOldDownloads(context: Context, maxAgeDays: Int = 30) {
        val dir = getDownloadDirectory(context)
        val cutoff = System.currentTimeMillis() - (maxAgeDays * 24 * 60 * 60 * 1000L)
        dir.listFiles()?.forEach { file ->
            if (file.lastModified() < cutoff) {
                file.delete()
            }
        }
    }
}
