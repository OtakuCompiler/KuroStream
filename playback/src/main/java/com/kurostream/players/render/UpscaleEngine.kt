package com.kurostream.players.render

import android.opengl.GLES20
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class UpscaleEngine {

    private val vertexShader = """
        attribute vec4 vPosition;
        attribute vec2 vTexCoord;
        varying vec2 texCoord;
        void main() {
            gl_Position = vPosition;
            texCoord = vTexCoord;
        }
    """.trimIndent()

    private val fragmentShader8K = """
        precision highp float;
        varying vec2 texCoord;
        uniform sampler2D sTexture;
        uniform vec2 uInputSize;
        uniform vec2 uOutputSize;
        uniform float uSharpness;
        uniform float uDenoise;
        uniform float uDetailBoost;
        uniform int uMode;

        float lanczos(float x, float a) {
            if (x == 0.0) return 1.0;
            if (abs(x) >= a) return 0.0;
            float pi = 3.14159265359;
            float pix = pi * x;
            return a * sin(pix) * sin(pix / a) / (pix * pix);
        }

        vec4 sampleLanczos6(sampler2D tex, vec2 uv, vec2 texelSize) {
            vec2 pixel = uv / texelSize;
            vec2 frac = fract(pixel);
            vec2 base = floor(pixel) * texelSize;
            vec4 result = vec4(0.0);
            float weightSum = 0.0;
            for (int x = -5; x <= 5; x++) {
                for (int y = -5; y <= 5; y++) {
                    vec2 offset = vec2(float(x), float(y));
                    float wx = lanczos(offset.x - frac.x + 0.5, 6.0);
                    float wy = lanczos(offset.y - frac.y + 0.5, 6.0);
                    float w = wx * wy;
                    result += texture2D(tex, base + offset * texelSize) * w;
                    weightSum += w;
                }
            }
            return result / weightSum;
        }

        void main() {
            vec2 texelSize = vec2(1.0) / uInputSize;
            vec4 color;
            if (uMode == 3) {
                color = sampleLanczos6(sTexture, texCoord, texelSize);
            } else if (uMode == 4) {
                color = texture2D(sTexture, texCoord);
            } else {
                color = sampleLanczos6(sTexture, texCoord, texelSize);
            }
            if (uSharpness > 0.0) {
                vec3 center = color.rgb;
                vec3 laplacian = -4.0 * center
                    + texture2D(sTexture, texCoord + vec2(texelSize.x, 0.0)).rgb
                    + texture2D(sTexture, texCoord - vec2(texelSize.x, 0.0)).rgb
                    + texture2D(sTexture, texCoord + vec2(0.0, texelSize.y)).rgb
                    + texture2D(sTexture, texCoord - vec2(0.0, texelSize.y)).rgb;
                float edge = length(laplacian);
                float mask = 1.0 - smoothstep(0.05, 0.2, edge);
                color.rgb = center - laplacian * uSharpness * mask;
            }
            gl_FragColor = vec4(clamp(color.rgb, 0.0, 1.0), color.a);
        }
    """.trimIndent()

    private var program: Int = 0
    private val vertexBuffer: FloatBuffer
    private val texCoordBuffer: FloatBuffer

    init {
        val vertices = floatArrayOf(-1f,-1f,0f, 1f,-1f,0f, -1f,1f,0f, 1f,1f,0f)
        val texCoords = floatArrayOf(0f,1f, 1f,1f, 0f,0f, 1f,0f)
        val vbb = ByteBuffer.allocateDirect(vertices.size * 4).order(ByteOrder.nativeOrder())
        vertexBuffer = vbb.asFloatBuffer().apply { put(vertices); position(0) }
        val tbb = ByteBuffer.allocateDirect(texCoords.size * 4).order(ByteOrder.nativeOrder())
        texCoordBuffer = tbb.asFloatBuffer().apply { put(texCoords); position(0) }
        program = createProgram(vertexShader, fragmentShader8K)
    }

    private fun createProgram(vs: String, fs: String): Int {
        val vShader = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER).apply {
            GLES20.glShaderSource(this, vs)
            GLES20.glCompileShader(this)
        }
        val fShader = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER).apply {
            GLES20.glShaderSource(this, fs)
            GLES20.glCompileShader(this)
        }
        return GLES20.glCreateProgram().apply {
            GLES20.glAttachShader(this, vShader)
            GLES20.glAttachShader(this, fShader)
            GLES20.glLinkProgram(this)
        }
    }

    fun render(
        inputTexture: Int,
        inputWidth: Int,
        inputHeight: Int,
        outputWidth: Int,
        outputHeight: Int,
        mode: Int = 2,
        sharpness: Float = 0.25f,
        denoise: Float = 0.3f,
        detailBoost: Float = 0.4f,
    ) {
        GLES20.glUseProgram(program)
        GLES20.glUniform2f(GLES20.glGetUniformLocation(program, "uInputSize"), inputWidth.toFloat(), inputHeight.toFloat())
        GLES20.glUniform2f(GLES20.glGetUniformLocation(program, "uOutputSize"), outputWidth.toFloat(), outputHeight.toFloat())
        GLES20.glUniform1f(GLES20.glGetUniformLocation(program, "uSharpness"), sharpness)
        GLES20.glUniform1f(GLES20.glGetUniformLocation(program, "uDenoise"), denoise)
        GLES20.glUniform1f(GLES20.glGetUniformLocation(program, "uDetailBoost"), detailBoost)
        GLES20.glUniform1i(GLES20.glGetUniformLocation(program, "uMode"), mode)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, inputTexture)
        GLES20.glUniform1i(GLES20.glGetUniformLocation(program, "sTexture"), 0)

        val posHandle = GLES20.glGetAttribLocation(program, "vPosition")
        val texHandle = GLES20.glGetAttribLocation(program, "vTexCoord")

        GLES20.glEnableVertexAttribArray(posHandle)
        GLES20.glVertexAttribPointer(posHandle, 3, GLES20.GL_FLOAT, false, 12, vertexBuffer)
        GLES20.glEnableVertexAttribArray(texHandle)
        GLES20.glVertexAttribPointer(texHandle, 2, GLES20.GL_FLOAT, false, 8, texCoordBuffer)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

        GLES20.glDisableVertexAttribArray(posHandle)
        GLES20.glDisableVertexAttribArray(texHandle)
    }
}
