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

package com.kurostream.data.subtitle

import android.content.Context
import com.kurostream.domain.result.Result
import com.kurostream.domain.subtitle.LanguagePair
import com.kurostream.domain.subtitle.SubtitleLine
import com.kurostream.domain.subtitle.TranslationResult
import com.kurostream.domain.subtitle.OfflineTranslator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OfflineTranslatorImpl @Inject constructor(
    private val context: Context,
) : OfflineTranslator {

    private var isInitialized = false

    override suspend fun initialize(): Result<Unit> = withContext(Dispatchers.IO) {
        isInitialized = true
        Result.success(Unit)
    }

    override suspend fun translate(
        text: String,
        sourceLang: String,
        targetLang: String
    ): Result<TranslationResult> = withContext(Dispatchers.IO) {
        if (!isInitialized) return@withContext Result.error(IllegalStateException("Translator not initialized"))
        Result.success(TranslationResult(
            originalText = text,
            translatedText = "[$sourceLang→$targetLang] $text",
            sourceLanguage = sourceLang,
            targetLanguage = targetLang,
            confidence = 0.5f,
            modelUsed = "stub"
        ))
    }

    override suspend fun translateBatch(
        lines: List<SubtitleLine>,
        sourceLang: String,
        targetLang: String
    ): Result<List<TranslationResult>> = withContext(Dispatchers.IO) {
        if (!isInitialized) return@withContext Result.error(IllegalStateException("Translator not initialized"))
        try {
            val results = lines.map { line ->
                translate(line.text, sourceLang, targetLang).getOrNull()
            }.filterNotNull()
            Result.success(results)
        } catch (e: Exception) {
            Result.error(e)
        }
    }

    override fun getSupportedLanguages(): Result<List<LanguagePair>> {
        return Result.success(listOf(
            LanguagePair("en", "zh", "English → Chinese"),
            LanguagePair("zh", "en", "Chinese → English"),
            LanguagePair("en", "es", "English → Spanish"),
            LanguagePair("es", "en", "Spanish → English"),
            LanguagePair("en", "fr", "English → French"),
            LanguagePair("fr", "en", "French → English"),
            LanguagePair("en", "de", "English → German"),
            LanguagePair("de", "en", "German → English"),
        ))
    }

    override fun isModelAvailable(sourceLang: String, targetLang: String): Boolean = isInitialized

    override suspend fun release() = withContext(Dispatchers.IO) {
        isInitialized = false
    }
}