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

package com.kurostream.domain.subtitle

import com.kurostream.common.result.Result
import kotlinx.coroutines.flow.Flow

interface OfflineTranslator {
    suspend fun initialize(): Result<Unit>
    suspend fun translate(text: String, sourceLang: String, targetLang: String): Result<TranslationResult>
    suspend fun translateBatch(lines: List<SubtitleLine>, sourceLang: String, targetLang: String): Result<List<TranslationResult>>
    fun getSupportedLanguages(): Result<List<LanguagePair>>
    fun isModelAvailable(sourceLang: String, targetLang: String): Boolean
    suspend fun release()
}

@Serializable
data class TranslationResult(
    val originalText: String,
    val translatedText: String,
    val sourceLanguage: String,
    val targetLanguage: String,
    val confidence: Float,
    val modelUsed: String,
)

@Serializable
data class LanguagePair(
    val sourceLang: String,
    val targetLang: String,
    val displayName: String,
)

@Serializable
data class SubtitleLine(
    val index: Int,
    val startTime: Long,
    val endTime: Long,
    val text: String,
    val language: String = "en",
)