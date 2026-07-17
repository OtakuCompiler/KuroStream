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

package com.kurostream.extensions.asr

import android.content.Context
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.SpeechService
import org.vosk.android.StorageService
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VoskRecognizer @Inject constructor(private val context: Context) {
    private var model: Model? = null
    private var speechService: SpeechService? = null
    private val _transcription = MutableStateFlow("")
    val transcription: StateFlow<String> = _transcription.asStateFlow()
    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()
    private val _partialResults = MutableStateFlow("")
    val partialResults: StateFlow<String> = _partialResults.asStateFlow()

    fun initializeModel(modelPath: String? = null): Flow<AsrState> = flow {
        emit(AsrState.Loading)
        try {
            val path = modelPath ?: run {
                val outputPath = File(context.filesDir, "model")
                if (!outputPath.exists()) {
                    StorageService.unpack(context, "model-en-us", "model", { }, { })
                }
                outputPath.absolutePath
            }
            model = Model(path)
            emit(AsrState.Ready)
        } catch (e: Exception) {
            emit(AsrState.Error(e.message ?: "Failed to load model"))
        }
    }

    fun startListening(sampleRate: Float = 16000f): Flow<AsrResult> = callbackFlow {
        val currentModel = model ?: run { trySend(AsrResult.Error("Model not initialized")); close(); return@callbackFlow }
        val recognizer = Recognizer(currentModel, sampleRate)
        speechService = SpeechService(recognizer, sampleRate.toInt())
        speechService?.startListening(object : org.vosk.android.RecognitionListener {
            override fun onPartialResult(hypothesis: String?) { hypothesis?.let { _partialResults.value = it } }
            override fun onResult(hypothesis: String?) { hypothesis?.let { _transcription.value += " $it"; trySend(AsrResult.Partial(it)) } }
            override fun onFinalResult(hypothesis: String?) { hypothesis?.let { _transcription.value += " $it"; trySend(AsrResult.Final(it)) }; _isListening.value = false }
            override fun onError(exception: Exception?) { trySend(AsrResult.Error(exception?.message ?: "Unknown error")); _isListening.value = false }
            override fun onTimeout() { _isListening.value = false }
        })
        _isListening.value = true
        awaitClose { speechService?.stop(); speechService = null }
    }

    fun stopListening() { speechService?.stop(); speechService = null; _isListening.value = false }
    fun resetTranscription() { _transcription.value = ""; _partialResults.value = "" }
    fun release() { stopListening(); model?.close(); model = null }
}

sealed class AsrState { object Loading : AsrState(); object Ready : AsrState(); data class Error(val message: String) : AsrState() }
sealed class AsrResult { data class Partial(val text: String) : AsrResult(); data class Final(val text: String) : AsrResult(); data class Error(val message: String) : AsrResult() }