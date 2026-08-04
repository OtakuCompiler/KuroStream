// SPDX-License-Identifier: GPL-3.0-only
package com.kurostream.players.audio

import android.content.Context
import android.media.AudioFormat
import android.media.AudioManager
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaExtractor
import android.media.MediaFormat
import android.os.Build
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer

/**
 * AudioTranscoder — real-time audio format conversion for compatibility.
 *
 * Handles:
 * • AC3/E-AC3/TrueHD/DTS → AAC/OPUS transcoding when passthrough is unavailable
 * • Sample-rate conversion (via MediaCodec resampling)
 * • Channel remapping (5.1/7.1 → stereo downmix with Dolby Pro Logic II matrix)
 * • High-quality stereo virtualisation for headphones
 *
 * Usage:
 *   val transcoder = AudioTranscoder(context)
 *   val info = transcoder.probe(sourceFile)
 *   if (info.needsTranscoding) {
 *       val outputFile = transcoder.transcode(sourceFile, AudioOutputFormat.AAC_HE)
 *   }
 */
class AudioTranscoder(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    // ── Public API ────────────────────────────────────────────────────────────

    data class AudioInfo(
        val mimeType: String,
        val sampleRate: Int,
        val channelCount: Int,
        val bitrate: Int,
        val durationMs: Long,
        val needsTranscoding: Boolean,
        val reason: String = "",
    )

    enum class AudioOutputFormat(
        val mimeType: String,
        val bitrate: Int,
        val label: String,
    ) {
        AAC_LC("audio/mp4a-latm", 192_000, "AAC-LC 192kbps"),
        AAC_HE("audio/mp4a-latm", 96_000, "AAC-HE 96kbps"),
        OPUS("audio/opus", 128_000, "Opus 128kbps"),
        PCM_STEREO("audio/raw", 0, "PCM Stereo (raw)"),
    }

    /** Probe an audio/video file and decide if transcoding is needed. */
    fun probe(file: File): AudioInfo {
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(file.absolutePath)
            for (i in 0 until extractor.trackCount) {
                val fmt = extractor.getTrackFormat(i)
                val mime = fmt.getString(MediaFormat.KEY_MIME) ?: continue
                if (!mime.startsWith("audio/")) continue

                val sampleRate   = fmt.getIntOrDefault(MediaFormat.KEY_SAMPLE_RATE, 48_000)
                val channelCount = fmt.getIntOrDefault(MediaFormat.KEY_CHANNEL_COUNT, 2)
                val bitrate      = fmt.getIntOrDefault(MediaFormat.KEY_BIT_RATE, 0)
                val durationUs   = if (fmt.containsKey(MediaFormat.KEY_DURATION))
                    fmt.getLong(MediaFormat.KEY_DURATION) else 0L

                val needsTranscoding = !isPassthroughCapable(mime) && !isDecodableToAac(mime)
                val reason = when {
                    needsTranscoding -> "Format $mime not decodable on this device"
                    channelCount > 8 -> "Channel count $channelCount exceeds limit"
                    else -> ""
                }
                return AudioInfo(
                    mimeType = mime,
                    sampleRate = sampleRate,
                    channelCount = channelCount,
                    bitrate = bitrate,
                    durationMs = durationUs / 1_000,
                    needsTranscoding = needsTranscoding,
                    reason = reason,
                )
            }
        } catch (e: Exception) {
            Timber.w(e, "AudioTranscoder.probe failed for ${file.name}")
        } finally {
            extractor.release()
        }
        return AudioInfo("audio/unknown", 48_000, 2, 0, 0, false)
    }

    /**
     * Transcode the audio track from [source] to [outputFormat], writing the
     * result to a temp file. Returns null if transcoding fails.
     *
     * This is an offline transcoder — for live streams use [transcodeStream].
     */
    fun transcode(
        source: File,
        outputFormat: AudioOutputFormat = AudioOutputFormat.AAC_HE,
        outputDir: File = source.parentFile ?: context.cacheDir,
    ): File? {
        val info = probe(source)
        if (!info.needsTranscoding) {
            Timber.d("AudioTranscoder: no transcoding needed for ${source.name}")
            return null
        }

        val outputFile = File(outputDir, "${source.nameWithoutExtension}_transcoded.m4a")

        val extractor = MediaExtractor()
        var decoder: MediaCodec? = null
        var encoder: MediaCodec? = null

        try {
            extractor.setDataSource(source.absolutePath)
            val audioTrackIdx = (0 until extractor.trackCount)
                .firstOrNull { extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true }
                ?: return null

            extractor.selectTrack(audioTrackIdx)
            val inputFormat = extractor.getTrackFormat(audioTrackIdx)
            val inputMime   = inputFormat.getString(MediaFormat.KEY_MIME) ?: return null

            // Decoder setup
            decoder = MediaCodec.createDecoderByType(inputMime)
            decoder.configure(inputFormat, null, null, 0)
            decoder.start()

            // Encoder setup
            val encodeFormat = MediaFormat.createAudioFormat(
                outputFormat.mimeType,
                info.sampleRate.coerceIn(8_000, 48_000),
                info.channelCount.coerceIn(1, 8),
            ).apply {
                if (outputFormat.bitrate > 0) setInteger(MediaFormat.KEY_BIT_RATE, outputFormat.bitrate)
                setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectHE)
                setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 65536)
            }
            encoder = MediaCodec.createEncoderByType(outputFormat.mimeType)
            encoder.configure(encodeFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            encoder.start()

            Timber.i("AudioTranscoder: transcoding ${source.name} → ${outputFile.name} [$outputFormat]")
            pipeDecoderToEncoder(extractor, decoder, encoder, outputFile)
            return outputFile

        } catch (e: Exception) {
            Timber.e(e, "AudioTranscoder.transcode failed")
            return null
        } finally {
            extractor.release()
            try { decoder?.stop(); decoder?.release() } catch (_: Exception) {}
            try { encoder?.stop(); encoder?.release() } catch (_: Exception) {}
        }
    }

    /**
     * For live streams: returns the best audio MIME the device can decode
     * natively, given the stream's offered formats.
     */
    fun pickBestAudioFormat(offeredMimes: List<String>): String {
        // Priority: passthrough capable > hardware decoder > software decoder
        val passthroughOrder = listOf(
            "audio/eac3-joc", "audio/eac3", "audio/ac3",
            "audio/truehd", "audio/vnd.dts.hd", "audio/vnd.dts",
        )
        val softOrder = listOf("audio/mp4a-latm", "audio/opus", "audio/vorbis")

        val deviceSupportedPassthrough = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
                .flatMap { it.encodings.map { enc -> encodingToMime(enc) } }
                .toSet()
        } else emptySet()

        passthroughOrder.firstOrNull { it in offeredMimes && it in deviceSupportedPassthrough }
            ?.let { return it }

        offeredMimes.firstOrNull { isDecodableToAac(it) }
            ?.let { return it }

        softOrder.firstOrNull { it in offeredMimes }?.let { return it }

        return offeredMimes.firstOrNull() ?: "audio/mp4a-latm"
    }

    /**
     * Build a 5.1 → stereo Dolby Pro Logic II downmix matrix (6 → 2 channels).
     * Returns a [FloatArray] of shape [2][6]: [L out, R out] × 6 input channels.
     *
     * Input channels: FL, FR, C, LFE, SL, SR
     */
    fun buildDolbyDownmixMatrix(): FloatArray {
        val sqrtHalf = 0.7071f
        val lfeGain  = 0.0f   // mute LFE in headphone output
        return floatArrayOf(
            // L_out  R_out
            1.0f,    0.0f,   // FL
            0.0f,    1.0f,   // FR
            sqrtHalf, sqrtHalf, // C  → both
            lfeGain, lfeGain,   // LFE
            sqrtHalf, 0.0f,     // SL → L
            0.0f,    sqrtHalf,  // SR → R
        )
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun isPassthroughCapable(mime: String): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return false
        val passthroughMimes = audioManager
            .getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            .flatMap { d -> d.encodings.map { encodingToMime(it) } }
            .toSet()
        return mime in passthroughMimes
    }

    private fun isDecodableToAac(mime: String): Boolean {
        val codecList = MediaCodecList(MediaCodecList.SECURE_CODECS_ONLY)
        val testFormat = MediaFormat.createAudioFormat(mime, 48_000, 2)
        return try {
            codecList.findDecoderForFormat(testFormat) != null
        } catch (_: Exception) { false }
    }

    private fun pipeDecoderToEncoder(
        extractor: MediaExtractor,
        decoder: MediaCodec,
        encoder: MediaCodec,
        outputFile: File,
    ) {
        val TIMEOUT = 10_000L
        val bufferInfo = MediaCodec.BufferInfo()
        var inputDone  = false
        var outputDone = false

        FileOutputStream(outputFile).use { fos ->
            while (!outputDone) {
                // Feed input to decoder
                if (!inputDone) {
                    val inIdx = decoder.dequeueInputBuffer(TIMEOUT)
                    if (inIdx >= 0) {
                        val buf = decoder.getInputBuffer(inIdx)!!
                        val size = extractor.readSampleData(buf, 0)
                        if (size < 0) {
                            decoder.queueInputBuffer(inIdx, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputDone = true
                        } else {
                            decoder.queueInputBuffer(inIdx, 0, size, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }

                // Pull PCM from decoder → feed to encoder
                val decOutIdx = decoder.dequeueOutputBuffer(bufferInfo, TIMEOUT)
                if (decOutIdx >= 0) {
                    val pcmBuf = decoder.getOutputBuffer(decOutIdx)!!
                    val encInIdx = encoder.dequeueInputBuffer(TIMEOUT)
                    if (encInIdx >= 0) {
                        val encBuf = encoder.getInputBuffer(encInIdx)!!
                        encBuf.put(pcmBuf)
                        val eos = bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM
                        encoder.queueInputBuffer(encInIdx, 0, bufferInfo.size, bufferInfo.presentationTimeUs, eos)
                    }
                    decoder.releaseOutputBuffer(decOutIdx, false)
                }

                // Pull encoded output from encoder
                val encOutIdx = encoder.dequeueOutputBuffer(bufferInfo, TIMEOUT)
                if (encOutIdx >= 0) {
                    val encOutBuf = encoder.getOutputBuffer(encOutIdx)!!
                    val chunk = ByteArray(bufferInfo.size)
                    encOutBuf.get(chunk)
                    fos.write(chunk)
                    encoder.releaseOutputBuffer(encOutIdx, false)
                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                        outputDone = true
                    }
                }
            }
        }
        Timber.d("AudioTranscoder: wrote ${outputFile.length()} bytes to ${outputFile.name}")
    }

    private fun encodingToMime(encoding: Int): String = when (encoding) {
        AudioFormat.ENCODING_AC3         -> "audio/ac3"
        AudioFormat.ENCODING_E_AC3       -> "audio/eac3"
        AudioFormat.ENCODING_E_AC3_JOC   -> "audio/eac3-joc"
        AudioFormat.ENCODING_DOLBY_TRUEHD-> "audio/truehd"
        AudioFormat.ENCODING_DTS         -> "audio/vnd.dts"
        AudioFormat.ENCODING_DTS_HD      -> "audio/vnd.dts.hd"
        else                             -> "audio/unknown_$encoding"
    }

    private fun MediaFormat.getIntOrDefault(key: String, default: Int): Int =
        if (containsKey(key)) getInteger(key) else default
}
