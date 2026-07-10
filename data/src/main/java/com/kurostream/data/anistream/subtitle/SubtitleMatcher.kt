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
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SubtitleMatcher @Inject constructor() {

    fun calculateHash(file: File): String {
        val chunkSize = 65536L
        val fileSize = file.length()

        if (fileSize < chunkSize) return ""

        val digest = MessageDigest.getInstance("MD5")

        file.inputStream().use { stream ->
            val firstChunk = ByteArray(chunkSize.toInt())
            stream.read(firstChunk)
            digest.update(firstChunk)

            stream.skip(fileSize - chunkSize * 2)
            val lastChunk = ByteArray(chunkSize.toInt())
            stream.read(lastChunk)
            digest.update(lastChunk)
        }

        val sizeBytes = fileSize.toString().toByteArray()
        digest.update(sizeBytes)

        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    fun findBestMatch(
        videoFileName: String,
        results: List<SubtitleResult>,
        preferredLanguage: String = "en"
    ): SubtitleResult? {
        val normalizedVideoName = normalizeFileName(videoFileName)

        val languageMatches = results.filter {
            it.language == preferredLanguage || it.language.startsWith(preferredLanguage)
        }.ifEmpty { results }

        val scored = languageMatches.map { result ->
            val normalizedSubName = normalizeFileName(result.filename)
            val similarity = calculateSimilarity(normalizedVideoName, normalizedSubName)
            result to similarity
        }

        return scored.maxByOrNull { it.second }?.first
    }

    fun scanLocalSubtitles(videoFile: File): List<File> {
        val directory = videoFile.parentFile ?: return emptyList()
        val baseName = videoFile.nameWithoutExtension

        val subtitleExtensions = listOf("srt", "ass", "ssa", "vtt", "sub")

        return directory.listFiles()?.filter { file ->
            file.isFile &&
            subtitleExtensions.any { ext -> file.extension.equals(ext, ignoreCase = true) } &&
            file.nameWithoutExtension.startsWith(baseName, ignoreCase = true)
        } ?: emptyList()
    }

    private fun normalizeFileName(name: String): String {
        return name
            .lowercase()
            .replace(Regex("[^\w]"), " ")
            .replace(Regex("\s+"), " ")
            .trim()
    }

    private fun calculateSimilarity(s1: String, s2: String): Double {
        val tokens1 = s1.split(" ").toSet()
        val tokens2 = s2.split(" ").toSet()

        val intersection = tokens1.intersect(tokens2).size
        val union = tokens1.union(tokens2).size

        return if (union == 0) 0.0 else intersection.toDouble() / union
    }
}
