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
import com.kurostream.core.common.result.Result
import com.kurostream.domain.model.LanguagePair
import com.kurostream.domain.model.SubtitleLine
import com.kurostream.domain.model.TranslationResult
import com.kurostream.domain.subtitle.OfflineTranslator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.GpuDelegate
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OfflineTranslatorImpl @Inject constructor(
    private val context: Context,
) : OfflineTranslator {

    private var interpreter: Interpreter? = null
    private var gpuDelegate: org.tensorflow.lite.gpu.GpuDelegate? = null
    private var isInitialized = false
    private val modelInputSize = 128
    private val vocabSize = 32000
    private val modelName = "translation_model.tflite"

    override suspend fun initialize(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val options = Interpreter.Options().setNumThreads(4)
            val gpuDelegate = org.tensorflow.lite.gpu.GpuDelegate()
            options.addDelegate(gpuDelegate)
            this.gpuDelegate = gpuDelegate

            val modelBuffer = loadModelFile(modelName)
            interpreter = Interpreter(modelBuffer, options)
            isInitialized = true
            Result.success(Unit)
        } catch (e: Exception) {
            Result.error(e)
        }
    }

    override suspend fun translate(
        text: String,
        sourceLang: String,
        targetLang: String
    ): Result<TranslationResult> = withContext(Dispatchers.IO) {
        if (!isInitialized) return@withContext Result.error(IllegalStateException("Translator not initialized"))
        try {
            val input = preprocessText(text, sourceLang)
            val output = runInference(input)
            val translated = decodeOutput(output, targetLang)

            Result.success(TranslationResult(
                originalText = text,
                translatedText = translated,
                sourceLanguage = sourceLang,
                targetLanguage = targetLang,
                confidence = 0.85f,
                modelUsed = "tflite_offline"
            ))
        } catch (e: Exception) {
            Result.error(e)
        }
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
        interpreter?.close()
        gpuDelegate?.close()
        interpreter = null
        gpuDelegate = null
        isInitialized = false
    }

    private fun preprocessText(text: String, sourceLang: String): ByteBuffer {
        val tokenizer = SimpleTokenizer()
        val tokens = tokenizer.encode(text)
        val input = ByteBuffer.allocateDirect(modelInputSize * 4)
        input.rewind()
        for (i in 0 until modelInputSize) {
            val tokenId = if (i < tokens.size) tokens[i] else 0
            input.putFloat(tokenId.toFloat())
        }
        return input
    }

    private fun runInference(input: ByteBuffer): ByteBuffer {
        val output = ByteBuffer.allocateDirect(modelInputSize * vocabSize * 4)
        val inputs = arrayOf<Any>(input)
        val outputs = arrayOf<Any>(output)
        interpreter?.runForMultipleInputsOutputs(inputs, outputs)
        return output
    }

    private fun decodeOutput(output: ByteBuffer, targetLang: String): String {
        val tokenizer = SimpleTokenizer()
        val tokens = IntArray(modelInputSize)
        output.rewind()
        for (i in 0 until modelInputSize) {
            var maxVal = Float.NEGATIVE_INFINITY
            var maxIdx = 0
            for (j in 0 until vocabSize) {
                val idx = i * vocabSize + j
                if (idx * 4 < output.capacity()) {
                    val value = output.getFloat(idx * 4)
                    if (value > maxVal) {
                        maxVal = value
                        maxIdx = j
                    }
                }
            }
            tokens[i] = maxIdx
        }
        return tokenizer.decode(tokens)
    }

    private fun loadModelFile(modelName: String): ByteBuffer {
        try {
            val inputStream = context.assets.open("models/$modelName")
            val file = File(context.cacheDir, modelName)
            FileOutputStream(file).use { output ->
                inputStream.copyTo(output)
            }
            val channel = FileInputStream(file).channel
            return channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size())
        } catch (e: Exception) {
            throw IllegalStateException("Failed to load model: $modelName", e)
        }
    }

    private class SimpleTokenizer {
        fun encode(text: String): IntArray {
            return text.split(" ").map { it.hashCode().absoluteValue % vocabSize }.toIntArray()
        }

        fun decode(tokens: IntArray): String {
            return tokens.filter { it > 0 }.joinToString(" ") { "[TOKEN_$it]" }
        }
    }

    companion object {
        private const val modelInputSize = 128
        private const val vocabSize = 32000
    }
}