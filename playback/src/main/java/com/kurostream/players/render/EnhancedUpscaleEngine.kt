// This file is part of KuroStream.
//
// EnhancedUpscaleEngine — production-grade OpenGL ES upscaler with
// selectable algorithms (bilinear/bicubic/lanczos) and quality presets.
// Shared by the existing UpscaleEngine and the new KuroVision pipeline.
//
// SPDX-License-Identifier: GPL-3.0-only
package com.kurostream.players.render

import android.opengl.GLES20
import android.opengl.GLES30
import android.util.Log
import com.kurostream.playback.kurovision.KuroVisionDeviceProfile
import com.kurostream.playback.kurovision.KuroVisionQualityMode
import com.kurostream.playback.kurovision.UpscaleAlgorithm
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class EnhancedUpscaleEngine(private val profile: KuroVisionDeviceProfile) {

    private var programBicubic = 0
    private var programLanczos = 0
    private var programPassthrough = 0
    private var currentProgram = 0

    private val vertexBuffer: FloatBuffer
    private val texCoordBuffer: FloatBuffer

    private var textureWidth = 0
    private var textureHeight = 0
    private var sharpness = 0f
    private var denoise = 0f
    private var detailBoost = 0f
    private var fakeHdrEnabled = false
    private var oledBlackEnabled = false

    companion object {
        private const val TAG = "EnhancedUpscaleEngine"
        private const val VERTEX_SHADER = """
            attribute vec4 vPosition;
            attribute vec2 vTexCoord;
            varying vec2 texCoord;
            void main() {
                gl_Position = vPosition;
                texCoord = vTexCoord;
            }
        """.trimIndent()
    }

    init {
        val vertices = floatArrayOf(
            -1f, -1f, 0f,
             1f, -1f, 0f,
            -1f,  1f, 0f,
             1f,  1f, 0f,
        )
        val texCoords = floatArrayOf(
            0f, 1f,
            1f, 1f,
            0f, 0f,
            1f, 0f,
        )
        val vb = ByteBuffer.allocateDirect(vertices.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
        vb.put(vertices).position(0)
        vertexBuffer = vb
        val tb = ByteBuffer.allocateDirect(texCoords.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
        tb.put(texCoords).position(0)
        texCoordBuffer = tb
    }

    fun initialize() {
        programPassthrough = createProgram(VERTEX_SHADER, FRAG_PASSTHROUGH)
        programBicubic = createProgram(VERTEX_SHADER, FRAG_BICUBIC)
        programLanczos = createProgram(VERTEX_SHADER, FRAG_LANCZOS)
        currentProgram = programPassthrough
    }

    fun setMode(mode: KuroVisionQualityMode, algo: UpscaleAlgorithm) {
        sharpness = if (mode.features.sharpening) 0.25f else 0f
        denoise = if (mode.features.denoise) 0.30f else 0f
        detailBoost = if (mode.features.animePro) 0.40f else 0f
        fakeHdrEnabled = mode.features.fakeHdr
        oledBlackEnabled = mode.features.oledBlack
        currentProgram = when (algo) {
            UpscaleAlgorithm.BILINEAR -> programPassthrough
            UpscaleAlgorithm.BICUBIC -> programBicubic
            UpscaleAlgorithm.LANCZOS3 -> programLanczos
            UpscaleAlgorithm.ULTRA -> programLanczos
        }
    }

    fun render(
        inputTexture: Int,
        inputWidth: Int,
        inputHeight: Int,
        outputWidth: Int,
        outputHeight: Int,
    ) {
        textureWidth = inputWidth
        textureHeight = inputHeight
        GLES20.glViewport(0, 0, outputWidth, outputHeight)
        GLES20.glUseProgram(currentProgram)
        GLES20.glUniform2f(GLES20.glGetUniformLocation(currentProgram, "uInputSize"), inputWidth.toFloat(), inputHeight.toFloat())
        GLES20.glUniform2f(GLES20.glGetUniformLocation(currentProgram, "uOutputSize"), outputWidth.toFloat(), outputHeight.toFloat())
        GLES20.glUniform1f(GLES20.glGetUniformLocation(currentProgram, "uSharpness"), sharpness)
        GLES20.glUniform1f(GLES20.glGetUniformLocation(currentProgram, "uDenoise"), denoise)
        GLES20.glUniform1f(GLES20.glGetUniformLocation(currentProgram, "uDetailBoost"), detailBoost)
        GLES20.glUniform1i(GLES20.glGetUniformLocation(currentProgram, "uMode"), if (fakeHdrEnabled) 1 else if (oledBlackEnabled) 2 else 0)
        GLES20.glUniform1f(GLES20.glGetUniformLocation(currentProgram, "uFakeHdrIntensity"), if (fakeHdrEnabled) 0.6f else 0f)
        GLES20.glUniform1f(GLES20.glGetUniformLocation(currentProgram, "uOledIntensity"), if (oledBlackEnabled) 0.55f else 0f)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, inputTexture)
        GLES20.glUniform1i(GLES20.glGetUniformLocation(currentProgram, "sTexture"), 0)

        val posHandle = GLES20.glGetAttribLocation(currentProgram, "vPosition")
        val texHandle = GLES20.glGetAttribLocation(currentProgram, "vTexCoord")

        GLES20.glEnableVertexAttribArray(posHandle)
        GLES20.glVertexAttribPointer(posHandle, 3, GLES20.GL_FLOAT, false, 12, vertexBuffer)
        GLES20.glEnableVertexAttribArray(texHandle)
        GLES20.glVertexAttribPointer(texHandle, 2, GLES20.GL_FLOAT, false, 8, texCoordBuffer)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

        GLES20.glDisableVertexAttribArray(posHandle)
        GLES20.glDisableVertexAttribArray(texHandle)
        checkGLError("render")
    }

    fun release() {
        GLES20.glDeleteProgram(programPassthrough)
        GLES20.glDeleteProgram(programBicubic)
        GLES20.glDeleteProgram(programLanczos)
    }

    private fun createProgram(vs: String, fs: String): Int {
        val v = compileShader(GLES20.GL_VERTEX_SHADER, vs)
        val f = compileShader(GLES20.GL_FRAGMENT_SHADER, fs)
        val prog = GLES20.glCreateProgram().also {
            GLES20.glAttachShader(it, v)
            GLES20.glAttachShader(it, f)
            GLES20.glLinkProgram(it)
            GLES20.glDeleteShader(v)
            GLES20.glDeleteShader(f)
        }
        val status = IntArray(1)
        GLES20.glGetProgramiv(prog, GLES20.GL_LINK_STATUS, status, 0)
        if (status[0] == 0) {
            Log.e(TAG, "Shader link error: ${GLES20.glGetProgramInfoLog(prog)}")
            GLES20.glDeleteProgram(prog)
            return 0
        }
        return prog
    }

    private fun compileShader(type: Int, source: String): Int {
        return GLES20.glCreateShader(type).also { shader ->
            GLES20.glShaderSource(shader, source)
            GLES20.glCompileShader(shader)
            val compiled = IntArray(1)
            GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0)
            if (compiled[0] == 0) {
                Log.e(TAG, "Shader compile error: ${GLES20.glGetShaderInfoLog(shader)}")
                GLES20.glDeleteShader(shader)
                throw IllegalStateException("Shader compile failed")
            }
        }
    }

    private fun checkGLError(op: String) {
        val err = GLES20.glGetError()
        if (err != GLES20.GL_NO_ERROR) {
            Log.w(TAG, "GL error after $op: 0x${err.toString(16)}")
        }
    }

    // ---- Shaders ----
    private val FRAG_PASSTHROUGH = """
        precision mediump float;
        varying vec2 texCoord;
        uniform sampler2D sTexture;
        void main() {
            gl_FragColor = texture2D(sTexture, texCoord);
        }
    """.trimIndent()

    private val FRAG_BICUBIC = """
        precision mediump float;
        varying vec2 texCoord;
        uniform sampler2D sTexture;
        uniform vec2 uInputSize;
        uniform float uSharpness;
        uniform float uDenoise;
        uniform vec2 uTexelSize;

        vec4 cubic(vec4 v0, vec4 v1, vec4 v2, vec4 v3, float t) {
            float t2 = t * t;
            float t3 = t2 * t;
            return 0.5 * (
                (2.0 * v1) +
                (-v0 + v2) * t +
                (2.0 * v0 - 5.0 * v1 + 4.0 * v2 - v3) * t2 +
                (-v0 + 3.0 * v1 - 3.0 * v2 + v3) * t3
            );
        }

        vec4 textureBicubic(sampler2D tex, vec2 uv, vec2 texelSize) {
            vec2 pixel = uv / texelSize - 0.5;
            vec2 f = fract(pixel);
            vec2 base = floor(pixel) * texelSize + 0.5 * texelSize;
            return cubic(
                cubic(texture2D(tex, base + vec2(-1.0, -1.0) * texelSize),
                      texture2D(tex, base + vec2( 0.0, -1.0) * texelSize),
                      texture2D(tex, base + vec2( 1.0, -1.0) * texelSize),
                      texture2D(tex, base + vec2( 2.0, -1.0) * texelSize), f.x),
                cubic(texture2D(tex, base + vec2(-1.0,  0.0) * texelSize),
                      texture2D(tex, base),
                      texture2D(tex, base + vec2( 1.0,  0.0) * texelSize),
                      texture2D(tex, base + vec2( 2.0,  0.0) * texelSize), f.x),
                cubic(texture2D(tex, base + vec2(-1.0,  1.0) * texelSize),
                      texture2D(tex, base + vec2( 0.0,  1.0) * texelSize),
                      texture2D(tex, base + vec2( 1.0,  1.0) * texelSize),
                      texture2D(tex, base + vec2( 2.0,  1.0) * texelSize), f.x),
                cubic(texture2D(tex, base + vec2(-1.0,  2.0) * texelSize),
                      texture2D(tex, base + vec2( 0.0,  2.0) * texelSize),
                      texture2D(tex, base + vec2( 1.0,  2.0) * texelSize),
                      texture2D(tex, base + vec2( 2.0,  2.0) * texelSize), f.x),
                f.y
            );
        }

        void main() {
            vec2 texelSize = vec2(1.0) / uInputSize;
            vec4 color = textureBicubic(sTexture, texCoord, texelSize);
            if (uSharpness > 0.0) {
                vec3 c = color.rgb;
                vec3 lap = -4.0 * c
                    + texture2D(sTexture, texCoord + vec2(texelSize.x, 0)).rgb
                    + texture2D(sTexture, texCoord - vec2(texelSize.x, 0)).rgb
                    + texture2D(sTexture, texCoord + vec2(0, texelSize.y)).rgb
                    + texture2D(sTexture, texCoord - vec2(0, texelSize.y)).rgb;
                float edge = length(lap);
                float mask = 1.0 - smoothstep(0.05, 0.25, edge);
                color.rgb = c - lap * uSharpness * mask * 0.5;
            }
            gl_FragColor = clamp(color, 0.0, 1.0);
        }
    """.trimIndent()

    private val FRAG_LANCZOS = """
        precision highp float;
        varying vec2 texCoord;
        uniform sampler2D sTexture;
        uniform vec2 uInputSize;
        uniform vec2 uOutputSize;
        uniform float uSharpness;
        uniform float uDenoise;
        uniform float uDetailBoost;
        uniform int uMode;
        uniform float uFakeHdrIntensity;
        uniform float uOledIntensity;

        float sinc(float x) {
            float pix = 3.14159265359 * x;
            return pix == 0.0 ? 1.0 : sin(pix) / pix;
        }
        float lanczos(float x, float a) {
            if (x == 0.0) return 1.0;
            if (abs(x) >= a) return 0.0;
            return sinc(x) * sinc(x / a);
        }

        vec4 sampleLanczos(sampler2D tex, vec2 uv, vec2 texelSize) {
            vec2 pixel = uv / texelSize;
            vec2 frac = fract(pixel);
            vec2 base = (floor(pixel) + 0.5) * texelSize;
            vec4 result = vec4(0.0);
            float weightSum = 0.0;
            float a = 3.0;
            for (int x = -3; x <= 3; x++) {
                for (int y = -3; y <= 3; y++) {
                    vec2 offset = vec2(float(x), float(y));
                    float wx = lanczos(offset.x - frac.x + 0.5, a);
                    float wy = lanczos(offset.y - frac.y + 0.5, a);
                    float w = wx * wy;
                    result += texture2D(tex, base + offset * texelSize) * w;
                    weightSum += w;
                }
            }
            return result / weightSum;
        }

        vec3 applyFakeHdr(vec3 c, float intensity) {
            vec3 mapped;
            mapped.r = c.r < 0.5 ? c.r * c.r * 1.2 : (1.0 - pow(2.0 - 2.0 * c.r, 2.0) * 0.5) * c.r;
            mapped.g = c.g < 0.5 ? c.g * c.g * 1.15 : c.g;
            mapped.b = c.b < 0.5 ? c.b * c.b * 1.1 : c.b;
            return mix(c, mapped, intensity);
        }

        vec3 applyOledBlack(vec3 c, float intensity) {
            float lum = dot(c, vec3(0.299, 0.587, 0.114));
            float boost = 1.0 + intensity * 0.3 * (1.0 - smoothstep(0.0, 0.15, lum));
            vec3 brightBoost = c * (1.0 + intensity * 0.15);
            return mix(c * boost, brightBoost, intensity * smoothstep(0.1, 0.6, lum));
        }

        void main() {
            vec2 texelSize = vec2(1.0) / uInputSize;
            vec4 color = sampleLanczos(sTexture, texCoord, texelSize);
            if (uSharpness > 0.0 && uDetailBoost > 0.0) {
                vec3 c = color.rgb;
                vec3 lap = -4.0 * c
                    + texture2D(sTexture, texCoord + vec2(texelSize.x, 0)).rgb
                    + texture2D(sTexture, texCoord - vec2(texelSize.x, 0)).rgb
                    + texture2D(sTexture, texCoord + vec2(0, texelSize.y)).rgb
                    + texture2D(sTexture, texCoord - vec2(0, texelSize.y)).rgb;
                float edge = length(lap);
                float mask = smoothstep(0.02, 0.15, edge) * uDetailBoost;
                color.rgb = c + lap * uSharpness * mask;
            }
            if (uMode == 1 && uFakeHdrIntensity > 0.0) {
                color.rgb = applyFakeHdr(color.rgb, uFakeHdrIntensity);
            }
            if (uMode == 2 && uOledIntensity > 0.0) {
                color.rgb = applyOledBlack(color.rgb, uOledIntensity);
            }
            gl_FragColor = clamp(color, 0.0, 1.0);
        }
    """.trimIndent()
}
