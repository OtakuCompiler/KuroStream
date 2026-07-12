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
import com.kurostream.domain.subtitle.OfflineTranslator
import com.kurostream.domain.subtitle.SubtitleLine
import com.kurostream.domain.subtitle.TranslationResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.gpu.GpuDelegate
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

@Singleton
class OfflineTranslatorImpl @Inject constructor(
    private val context: Context,
) : OfflineTranslator {

    private var interpreter: Interpreter? = null
    private var gpuDelegate: GpuDelegate? = null
    private var isInitialized = false
    private val modelInputSize = 512 // Max token length
    private val vocabSize = 32000 // SentencePiece vocab size

    override suspend fun initialize(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (isInitialized) return@withContext Result.success(Unit)

            // Load TFLite model from assets
            val modelFile = copyModelFromAssets("translator_model.tflite")
            val options = Interpreter.Options().apply {
                setNumThreads(4)
                // Try GPU delegate
                val compatList = CompatibilityList()
                if (compatList.isDelegateSupportedOnThisDevice()) {
                    gpuDelegate = compatList.bestOptions?.let { GpuDelegate(it) }
                    gpuDelegate?.let { addDelegate(it) }
                }
            }

            interpreter = Interpreter(modelFile, options)
            isInitialized = true
            Timber.d("Offline translator initialized with GPU: ${gpuDelegate != null}")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize offline translator")
            Result.failure(e)
        }
    }

    override suspend fun translate(
        text: String,
        sourceLang: String,
        targetLang: String,
    ): Result<TranslationResult> = withContext(Dispatchers.IO) {
        if (!isInitialized) {
            val initResult = initialize()
            if (initResult.isFailure) return@withContext initResult
        }

        try {
            val tokenizer = SimpleTokenizer()
            val tokens = tokenizer.encode(text)
            
            // Truncate or pad to model input size
            val inputTokens = if (tokens.size > modelInputSize) {
                tokens.take(modelInputSize)
            } else {
                tokens + List(modelInputSize - tokens.size) { 0 } // Pad with 0
            }

            // Prepare input tensor
            val inputBuffer = ByteBuffer.allocateDirect(1 * modelInputSize * 4).apply {
                order(ByteOrder.nativeOrder())
                inputTokens.forEach { putInt(it) }
            }

            // Prepare output tensor
            val outputBuffer = ByteBuffer.allocateDirect(1 * modelInputSize * vocabSize * 4).apply {
                order(ByteOrder.nativeOrder())
            }

            // Run inference
            interpreter?.run(inputBuffer, outputBuffer)

            // Decode output (simplified - in reality would use beam search)
            val translatedTokens = decodeOutput(outputBuffer)
            val translatedText = tokenizer.decode(translatedTokens)

            Result.success(TranslationResult(
                originalText = text,
                translatedText = translatedText,
                sourceLanguage = sourceLang,
                targetLanguage = targetLang,
                confidence = 0.85f, // Would calculate from logits
                modelUsed = "offline_transformer",
            ))
        } catch (e: Exception) {
            Timber.e(e, "Translation failed")
            Result.failure(e)
        }
    }

    override suspend fun translateBatch(
        lines: List<SubtitleLine>,
        sourceLang: String,
        targetLang: String,
    ): Result<List<TranslationResult>> = withContext(Dispatchers.IO) {
        val results = mutableListOf<TranslationResult>()
        for (line in lines) {
            translate(line.text, sourceLang, targetLang)
                .onSuccess { results.add(it) }
                .onFailure { 
                    // Add original text on failure
                    results.add(TranslationResult(
                        originalText = line.text,
                        translatedText = line.text,
                        sourceLanguage = sourceLang,
                        targetLanguage = targetLang,
                        confidence = 0f,
                        modelUsed = "fallback",
                    ))
                }
        }
        Result.success(results)
    }

    override fun getSupportedLanguages(): Result<List<LanguagePair>> = Result.success(listOf(
        LanguagePair("en", "ja", "English → Japanese"),
        LanguagePair("ja", "en", "Japanese → English"),
        LanguagePair("en", "ko", "English → Korean"),
        LanguagePair("ko", "en", "Korean → English"),
        LanguagePair("en", "zh", "English → Chinese"),
        LanguagePair("zh", "en", "Chinese → English"),
        LanguagePair("en", "es", "English → Spanish"),
        LanguagePair("es", "en", "Spanish → English"),
        LanguagePair("en", "fr", "English → French"),
        LanguagePair("fr", "en", "French → English"),
        LanguagePair("en", "de", "English → German"),
        LanguagePair("de", "en", "German → English"),
    ))

    override fun isModelAvailable(sourceLang: String, targetLang: String): Boolean = isInitialized

    override suspend fun release() = withContext(Dispatchers.IO) {
        interpreter?.close()
        gpuDelegate?.close()
        interpreter = null
        gpuDelegate = null
        isInitialized = false
    }

    private fun copyModelFromAssets(modelName: String): ByteBuffer {
        val inputStream = context.assets.open("models/$modelName")
        val file = File(context.cacheDir, modelName)
        file.outputStream().use { output ->
            inputStream.copyTo(output)
        }
        val channel = FileInputStream(file).channel
        return channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size())
    }

    private fun decodeOutput(buffer: ByteBuffer): IntArray {
        val output = IntArray(modelInputSize)
        buffer.rewind()
        for (i in 0 until modelInputSize) {
            output[i] = findMaxIndex(buffer, i)
        }
        return output
    }

    private fun findMaxIndex(buffer: ByteBuffer, i: Int): Int {
        var maxVal = Float.NEGATIVE_INFINITY
        var maxIdx = 0
        for (j in 0 until vocabSize) {
            val idx = i * vocabSize + j
            if (idx * 4 < buffer.capacity()) {
                val value = buffer.getFloat(idx * 4)
                if (value > maxVal) {
                    maxVal = value
                    maxIdx = j
                }
            }
        }
        return maxIdx
    }

    // Simple tokenizer placeholder - would use SentencePiece in production
    private class SimpleTokenizer {
        fun encode(text: String): IntArray {
            // Simplified: split by whitespace and map to vocab
            return text.split(" ").map { it.hashCode().absoluteValue % vocabSize }.toIntArray()
        }

        fun decode(tokens: IntArray): String {
            // Simplified: just join tokens
            return tokens.filter { it > 0 }.joinToString(" ") { "[TOKEN_$it]" }
        }
    }
}