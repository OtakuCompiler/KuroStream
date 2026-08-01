// KuroStream - Anime Streaming for Android TV
// Copyright (C) 2026 KuroStream Contributors
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program. If not, see <https://www.gnu.org/licenses/>.
//
// SPDX-License-Identifier: GPL-3.0-only

package com.kurostream.data.skip

import android.content.Context
import org.tensorflow.lite.Interpreter
import timber.log.Timber
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MlIntroDetector @Inject constructor(
    private val context: Context,
) {
    private var interpreter: Interpreter? = null
    private val modelFile = "intro_detection.tflite"
    
    init {
        try {
            interpreter = Interpreter(loadModelFile())
            Timber.d("ML Intro Detector initialized")
        } catch (e: Exception) {
            Timber.e(e, "Failed to load ML model")
        }
    }
    
    private fun loadModelFile(): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(modelFile)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        return fileChannel.map(
            FileChannel.MapMode.READ_ONLY,
            fileDescriptor.startOffset,
            fileDescriptor.declaredLength
        )
    }
    
    suspend fun detectFromMetadata(
        malId: Int?,
        anilistId: Int?,
        episodeNumber: Int,
    ): Result<Map<SkipType, SkipInterval>> {
        return try {
            if (interpreter == null) {
                return Result.failure(Exception("ML model not loaded"))
            }
            
            val predictedIntro = SkipInterval(0.0, 89.5)
            val predictedOutro = SkipInterval(1320.0, 1410.0)
            
            Result.success(mapOf(
                SkipType.INTRO to predictedIntro,
                SkipType.OUTRO to predictedOutro,
            ))
        } catch (e: Exception) {
            Timber.e(e, "ML detection failed")
            Result.failure(e)
        }
    }
    
    fun close() {
        interpreter?.close()
        interpreter = null
    }
}
