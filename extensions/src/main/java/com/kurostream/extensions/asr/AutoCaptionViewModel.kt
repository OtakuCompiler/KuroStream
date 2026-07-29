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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AutoCaptionViewModel @Inject constructor(
    private val voskRecognizer: VoskRecognizer,
    private val androidRecognizer: AndroidSpeechRecognizer
) : ViewModel() {

    private val _captionText = MutableStateFlow("")
    val captionText: StateFlow<String> = _captionText.asStateFlow()
    private val _partialText = MutableStateFlow("")
    val partialText: StateFlow<String> = _partialText.asStateFlow()
    val isListening: StateFlow<Boolean> = voskRecognizer.isListening

    init {
        viewModelScope.launch { voskRecognizer.partialResults.collect { _partialText.value = it } }
        viewModelScope.launch { voskRecognizer.transcription.collect { _captionText.value = it } }
    }

    fun startListening() {
        viewModelScope.launch {
            voskRecognizer.startListening().collect { result ->
                when (result) {
                    is AsrResult.Partial -> _partialText.value = result.text
                    is AsrResult.Final -> { _captionText.value += " ${result.text}"; _partialText.value = "" }
                    is AsrResult.Error -> {
                        launchFallbackRecognizer()
                        return@collect
                    }
                }
            }
        }
    }

    private fun launchFallbackRecognizer() {
        viewModelScope.launch {
            androidRecognizer.startListening().collect { speechResult ->
                when (speechResult) {
                    is SpeechResult.Partial -> _partialText.value = speechResult.text
                    is SpeechResult.Success -> { _captionText.value += " ${speechResult.text}"; _partialText.value = "" }
                    is SpeechResult.Error -> {}
                }
            }
        }
    }

    fun stopListening() { voskRecognizer.stopListening(); androidRecognizer.stopListening() }
    fun clearCaptions() { _captionText.value = ""; _partialText.value = ""; voskRecognizer.resetTranscription() }

    override fun onCleared() { super.onCleared(); voskRecognizer.release(); androidRecognizer.destroy() }
}
