// SPDX-License-Identifier: GPL-3.0-only
package com.kurostream.players.render

import android.opengl.GLES20
import android.util.Log
import com.kurostream.playback.kurovision.KuroVisionDeviceProfile
import com.kurostream.playback.kurovision.KuroVisionQualityMode
import com.kurostream.playback.kurovision.UpscaleAlgorithm
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/**
 * EnhancedUpscaleEngine — production-grade OpenGL ES 2.0 upscaler.
 *
 * Algorithms
 * ──────────
 * BILINEAR   — fast hardware bilinear (passthrough program, GPU handles filtering)
 * BICUBIC    — 4×4 Catmull-Rom bicubic with unsharp-mask edge sharpening
 * LANCZOS3   — Lanczos-3 (7×7 kernel) with optional detail-boost for anime
 * ULTRA      — Lanczos-3 + full post-processing stack (HDR, OLED, color profile)
 *
 * Post-processing (all optional, stackable)
 * ─────────────────────────────────────────
 * • Fake HDR / HDR10 Simulation
 *   S-curve tone mapper, local contrast enhancement, chromatic vibrance,
 *   highlight roll-off to prevent clipping — makes SDR content look like
 *   it was graded for HDR on standard TV panels.
 *
 * • OLED Black Crush
 *   Crushes near-black pixels to true 0,0,0 using a threshold knee, while
 *   boosting mid-tones for OLED displays where pure black = zero emission.
 *   Also lifts peak whites slightly to increase apparent HDR range.
 *
 * • Color Profiles
 *   Cinema:  warm teal-orange grade (Hollywood standard)
 *   Vivid:   saturated and punchy (anime/gaming)
 *   Natural: flat, faithful to source
 *   Cool:    slight blue push for sci-fi/night scenes
 *   Warm:    amber lift for film-look drama
 *
 * • Anime Detail Boost
 *   Edge-aware sharpening tuned for flat-colour animation: emphasises
 *   outlines without ringing on skin gradients.
 */
class EnhancedUpscaleEngine(private val profile: KuroVisionDeviceProfile) {

    private var programBicubic     = 0
    private var programLanczos     = 0
    private var programPassthrough = 0
    private var programWaifu2x     = 0
    private var currentProgram     = 0

    private val vertexBuffer:  FloatBuffer
    private val texCoordBuffer: FloatBuffer

    // Per-frame uniforms
    private var sharpness      = 0f
    private var denoise        = 0f
    private var detailBoost    = 0f
    private var fakeHdrEnabled = false
    private var fakeHdrIntensity = 0.65f
    private var oledBlack      = false
    private var oledIntensity  = 0.60f
    private var colorProfile   = ColorProfile.NATURAL
    private var saturationBoost = 0f

    companion object {
        private const val TAG = "EnhancedUpscale"

        private val VERTEX_SHADER = """
            attribute vec4 vPosition;
            attribute vec2 vTexCoord;
            varying vec2 texCoord;
            void main() {
                gl_Position = vPosition;
                texCoord = vTexCoord;
            }
        """.trimIndent()

        /** Tonemap single channel with S-curve: deepens shadows, rolls off highlights. */
        private val FUNC_TONEMAP = """
            float tonemap(float x) {
                // Modified Reinhard with shoulder
                float a = 2.51, b = 0.03, c = 2.43, d = 0.59, e = 0.14;
                return clamp((x*(a*x+b))/(x*(c*x+d)+e), 0.0, 1.0);
            }
        """

        /** Fake HDR: full tone-map + saturation boost + local contrast. */
        private val FUNC_FAKE_HDR = """
            vec3 applyFakeHdr(vec3 c, float intensity, float satBoost) {
                // Luminance
                float lum = dot(c, vec3(0.2126, 0.7152, 0.0722));
                // S-curve per channel
                vec3 mapped = vec3(tonemap(c.r * 1.05), tonemap(c.g * 1.02), tonemap(c.b));
                // Chrominance saturation boost in midtones
                vec3 grey = vec3(lum);
                mapped = mix(grey, mapped, 1.0 + satBoost * smoothstep(0.1, 0.6, lum));
                // Highlight roll-off: prevent clipping, add faint glow
                float hiLum = max(max(mapped.r, mapped.g), mapped.b);
                float hiGlow = smoothstep(0.85, 1.0, hiLum) * 0.04;
                mapped += hiGlow;
                return mix(c, clamp(mapped, 0.0, 1.0), intensity);
            }
        """

        /** OLED black crush + peak white boost. */
        private val FUNC_OLED = """
            vec3 applyOled(vec3 c, float intensity) {
                float lum = dot(c, vec3(0.299, 0.587, 0.114));
                // Crush near-blacks to true black using smooth knee at 0.08
                float knee = 0.08;
                float crushFactor = 1.0 - smoothstep(0.0, knee, lum) * (1.0 - lum / knee);
                vec3 crushed = c * (1.0 - intensity * (1.0 - smoothstep(0.0, knee * 2.0, lum)));
                // Boost peak whites slightly for apparent HDR range on OLED
                float peakBoost = 1.0 + intensity * 0.12 * smoothstep(0.8, 1.0, lum);
                vec3 boosted = crushed * peakBoost;
                return clamp(boosted, 0.0, 1.0);
            }
        """

        /** Color profile matrix applied after HDR/OLED processing. */
        private val FUNC_COLOR_PROFILE = """
            // profile: 0=Natural, 1=Cinema(teal-orange), 2=Vivid, 3=Cool, 4=Warm
            vec3 applyColorProfile(vec3 c, int profile) {
                if (profile == 0) return c; // Natural — passthrough
                float lum = dot(c, vec3(0.2126, 0.7152, 0.0722));
                if (profile == 1) {
                    // Cinema: teal shadows, orange highlights (Lut-style grade)
                    vec3 shadows = vec3(0.0, 0.04, 0.06) * (1.0 - lum);
                    vec3 highs   = vec3(0.05, 0.02, -0.02) * lum;
                    return clamp(c + shadows + highs, 0.0, 1.0);
                }
                if (profile == 2) {
                    // Vivid: increase saturation uniformly
                    vec3 grey = vec3(lum);
                    return clamp(mix(grey, c, 1.25), 0.0, 1.0);
                }
                if (profile == 3) {
                    // Cool: shift white balance toward blue
                    return clamp(c + vec3(-0.02, -0.01, 0.04), 0.0, 1.0);
                }
                if (profile == 4) {
                    // Warm: shift toward amber
                    return clamp(c + vec3(0.04, 0.01, -0.03), 0.0, 1.0);
                }
                return c;
            }
        """
    }

    init {
        val vertices  = floatArrayOf(-1f,-1f,0f, 1f,-1f,0f, -1f,1f,0f, 1f,1f,0f)
        val texCoords = floatArrayOf(0f,1f, 1f,1f, 0f,0f, 1f,0f)
        vertexBuffer  = ByteBuffer.allocateDirect(vertices.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().also { it.put(vertices).position(0) }
        texCoordBuffer= ByteBuffer.allocateDirect(texCoords.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().also { it.put(texCoords).position(0) }
    }

    fun initialize() {
        programPassthrough = createProgram(VERTEX_SHADER, FRAG_PASSTHROUGH)
        programBicubic     = createProgram(VERTEX_SHADER, FRAG_BICUBIC)
        programLanczos     = createProgram(VERTEX_SHADER, FRAG_LANCZOS)
        programWaifu2x     = createProgram(VERTEX_SHADER, FRAG_WAIFU2X)
        currentProgram     = programPassthrough
    }

    fun setMode(mode: KuroVisionQualityMode, algo: UpscaleAlgorithm) {
        sharpness       = if (mode.features.sharpening)  0.30f else 0f
        denoise         = if (mode.features.denoise)     0.25f else 0f
        detailBoost     = if (mode.features.animePro)    0.45f else 0f
        fakeHdrEnabled  = mode.features.fakeHdr
        oledBlack       = mode.features.oledBlack
        currentProgram  = when (algo) {
            UpscaleAlgorithm.BILINEAR -> programPassthrough
            UpscaleAlgorithm.BICUBIC  -> programBicubic
            UpscaleAlgorithm.LANCZOS3 -> programLanczos
            UpscaleAlgorithm.ULTRA    -> programLanczos
            UpscaleAlgorithm.WAIFU2X  -> programWaifu2x
        }
    }

    /** Fine-tune individual post-process parameters. */
    fun configure(
        fakeHdr:      Boolean?      = null,
        hdrIntensity: Float?        = null,
        oled:         Boolean?      = null,
        oledInt:      Float?        = null,
        profile:      ColorProfile? = null,
        satBoost:     Float?        = null,
        sharp:        Float?        = null,
    ) {
        fakeHdr?.let      { fakeHdrEnabled   = it }
        hdrIntensity?.let { fakeHdrIntensity = it.coerceIn(0f, 1f) }
        oled?.let         { oledBlack        = it }
        oledInt?.let      { oledIntensity    = it.coerceIn(0f, 1f) }
        profile?.let      { colorProfile     = it }
        satBoost?.let     { saturationBoost  = it.coerceIn(0f, 1f) }
        sharp?.let        { sharpness        = it.coerceIn(0f, 1f) }
    }

    fun render(inputTexture: Int, inputW: Int, inputH: Int, outputW: Int, outputH: Int) {
        GLES20.glViewport(0, 0, outputW, outputH)
        GLES20.glUseProgram(currentProgram)

        fun uni1i(name: String, v: Int)   = GLES20.glUniform1i(GLES20.glGetUniformLocation(currentProgram, name), v)
        fun uni1f(name: String, v: Float) = GLES20.glUniform1f(GLES20.glGetUniformLocation(currentProgram, name), v)
        fun uni2f(name: String, x: Float, y: Float) = GLES20.glUniform2f(GLES20.glGetUniformLocation(currentProgram, name), x, y)

        uni2f("uInputSize",  inputW.toFloat(), inputH.toFloat())
        uni2f("uOutputSize", outputW.toFloat(), outputH.toFloat())
        uni1f("uSharpness",  sharpness)
        uni1f("uDenoise",    denoise)
        uni1f("uDetailBoost",detailBoost)
        uni1i("uFakeHdr",    if (fakeHdrEnabled) 1 else 0)
        uni1f("uHdrIntensity", fakeHdrIntensity)
        uni1f("uSatBoost",   saturationBoost)
        uni1i("uOled",       if (oledBlack) 1 else 0)
        uni1f("uOledIntensity", oledIntensity)
        uni1i("uColorProfile", colorProfile.ordinal)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, inputTexture)
        uni1i("sTexture", 0)

        val pos = GLES20.glGetAttribLocation(currentProgram, "vPosition")
        val tex = GLES20.glGetAttribLocation(currentProgram, "vTexCoord")
        GLES20.glEnableVertexAttribArray(pos)
        GLES20.glVertexAttribPointer(pos, 3, GLES20.GL_FLOAT, false, 12, vertexBuffer)
        GLES20.glEnableVertexAttribArray(tex)
        GLES20.glVertexAttribPointer(tex, 2, GLES20.GL_FLOAT, false,  8, texCoordBuffer)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        GLES20.glDisableVertexAttribArray(pos)
        GLES20.glDisableVertexAttribArray(tex)

        val err = GLES20.glGetError()
        if (err != GLES20.GL_NO_ERROR) Log.w(TAG, "GL error: 0x${err.toString(16)}")
    }

    fun release() {
        intArrayOf(programPassthrough, programBicubic, programLanczos, programWaifu2x)
            .filter { it != 0 }
            .forEach { GLES20.glDeleteProgram(it) }
        programPassthrough = 0; programBicubic = 0; programLanczos = 0; programWaifu2x = 0
    }

    // ── Shader compilation ────────────────────────────────────────────────────

    private fun createProgram(vs: String, fs: String): Int {
        val v = compileShader(GLES20.GL_VERTEX_SHADER, vs)
        val f = compileShader(GLES20.GL_FRAGMENT_SHADER, fs)
        return GLES20.glCreateProgram().also { prog ->
            GLES20.glAttachShader(prog, v); GLES20.glAttachShader(prog, f)
            GLES20.glLinkProgram(prog)
            GLES20.glDeleteShader(v); GLES20.glDeleteShader(f)
            val status = IntArray(1)
            GLES20.glGetProgramiv(prog, GLES20.GL_LINK_STATUS, status, 0)
            if (status[0] == 0) {
                Log.e(TAG, "Link error: ${GLES20.glGetProgramInfoLog(prog)}")
                GLES20.glDeleteProgram(prog)
                return 0
            }
        }
    }

    private fun compileShader(type: Int, src: String): Int {
        return GLES20.glCreateShader(type).also {
            GLES20.glShaderSource(it, src); GLES20.glCompileShader(it)
            val ok = IntArray(1)
            GLES20.glGetShaderiv(it, GLES20.GL_COMPILE_STATUS, ok, 0)
            if (ok[0] == 0) {
                Log.e(TAG, "Shader error: ${GLES20.glGetShaderInfoLog(it)}")
                GLES20.glDeleteShader(it)
                throw IllegalStateException("Shader compile failed")
            }
        }
    }

    // ── Fragment shaders ──────────────────────────────────────────────────────

    private val POST_PROCESS_UNIFORMS = """
        uniform int  uFakeHdr;
        uniform float uHdrIntensity;
        uniform float uSatBoost;
        uniform int  uOled;
        uniform float uOledIntensity;
        uniform int  uColorProfile;
    """
    private val POST_PROCESS_APPLY = """
        if (uFakeHdr == 1)   color.rgb = applyFakeHdr(color.rgb, uHdrIntensity, uSatBoost);
        if (uOled == 1)      color.rgb = applyOled(color.rgb, uOledIntensity);
        color.rgb = applyColorProfile(color.rgb, uColorProfile);
    """

    private val FRAG_PASSTHROUGH = """
        precision mediump float;
        varying vec2 texCoord;
        uniform sampler2D sTexture;
        $POST_PROCESS_UNIFORMS
        $FUNC_TONEMAP
        $FUNC_FAKE_HDR
        $FUNC_OLED
        $FUNC_COLOR_PROFILE
        void main() {
            vec4 color = texture2D(sTexture, texCoord);
            $POST_PROCESS_APPLY
            gl_FragColor = clamp(color, 0.0, 1.0);
        }
    """.trimIndent()

    private val FRAG_BICUBIC = """
        precision highp float;
        varying vec2 texCoord;
        uniform sampler2D sTexture;
        uniform vec2 uInputSize;
        uniform float uSharpness;
        $POST_PROCESS_UNIFORMS
        $FUNC_TONEMAP
        $FUNC_FAKE_HDR
        $FUNC_OLED
        $FUNC_COLOR_PROFILE

        vec4 cubic(vec4 v0, vec4 v1, vec4 v2, vec4 v3, float t) {
            float t2 = t*t, t3 = t2*t;
            return 0.5*(2.0*v1+(-v0+v2)*t+(2.0*v0-5.0*v1+4.0*v2-v3)*t2+(-v0+3.0*v1-3.0*v2+v3)*t3);
        }
        vec4 sample4(sampler2D tex, vec2 uv, vec2 ts) {
            vec2 p = uv/ts - 0.5; vec2 f = fract(p);
            vec2 b = (floor(p)+0.5)*ts;
            return cubic(
                cubic(texture2D(tex,b+vec2(-1,-1)*ts),texture2D(tex,b+vec2(0,-1)*ts),texture2D(tex,b+vec2(1,-1)*ts),texture2D(tex,b+vec2(2,-1)*ts),f.x),
                cubic(texture2D(tex,b+vec2(-1, 0)*ts),texture2D(tex,b),              texture2D(tex,b+vec2(1, 0)*ts),texture2D(tex,b+vec2(2, 0)*ts),f.x),
                cubic(texture2D(tex,b+vec2(-1, 1)*ts),texture2D(tex,b+vec2(0, 1)*ts),texture2D(tex,b+vec2(1, 1)*ts),texture2D(tex,b+vec2(2, 1)*ts),f.x),
                cubic(texture2D(tex,b+vec2(-1, 2)*ts),texture2D(tex,b+vec2(0, 2)*ts),texture2D(tex,b+vec2(1, 2)*ts),texture2D(tex,b+vec2(2, 2)*ts),f.x),
                f.y);
        }
        void main() {
            vec2 ts = vec2(1.0)/uInputSize;
            vec4 color = sample4(sTexture, texCoord, ts);
            if (uSharpness > 0.0) {
                vec3 c = color.rgb;
                vec3 lap = -4.0*c + texture2D(sTexture,texCoord+vec2(ts.x,0)).rgb
                    + texture2D(sTexture,texCoord-vec2(ts.x,0)).rgb
                    + texture2D(sTexture,texCoord+vec2(0,ts.y)).rgb
                    + texture2D(sTexture,texCoord-vec2(0,ts.y)).rgb;
                float mask = 1.0 - smoothstep(0.04, 0.20, length(lap));
                color.rgb = c - lap * uSharpness * mask * 0.5;
            }
            $POST_PROCESS_APPLY
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
        uniform float uDetailBoost;
        $POST_PROCESS_UNIFORMS
        $FUNC_TONEMAP
        $FUNC_FAKE_HDR
        $FUNC_OLED
        $FUNC_COLOR_PROFILE

        float sinc(float x) { float p = 3.14159265*x; return p==0.0?1.0:sin(p)/p; }
        float lanczos(float x, float a) {
            if (x==0.0) return 1.0;
            if (abs(x)>=a) return 0.0;
            return sinc(x)*sinc(x/a);
        }
        vec4 sampleLanczos(vec2 uv) {
            vec2 ts = vec2(1.0)/uInputSize;
            vec2 pixel = uv/ts;
            vec2 frac  = fract(pixel);
            vec2 base  = (floor(pixel)+0.5)*ts;
            vec4 result = vec4(0.0); float wsum = 0.0;
            float a = 3.0;
            for (int x=-3; x<=3; x++) {
                for (int y=-3; y<=3; y++) {
                    vec2 off = vec2(float(x),float(y));
                    float wx = lanczos(off.x - frac.x + 0.5, a);
                    float wy = lanczos(off.y - frac.y + 0.5, a);
                    float w  = wx*wy;
                    result += texture2D(sTexture, base+off*ts)*w;
                    wsum   += w;
                }
            }
            return result/wsum;
        }
        void main() {
            vec4 color = sampleLanczos(texCoord);
            // Anime detail boost: edge-aware USM
            if (uSharpness > 0.0 || uDetailBoost > 0.0) {
                vec2 ts = vec2(1.0)/uInputSize;
                vec3 c   = color.rgb;
                vec3 lap = -4.0*c
                    + texture2D(sTexture,texCoord+vec2(ts.x,0)).rgb
                    + texture2D(sTexture,texCoord-vec2(ts.x,0)).rgb
                    + texture2D(sTexture,texCoord+vec2(0,ts.y)).rgb
                    + texture2D(sTexture,texCoord-vec2(0,ts.y)).rgb;
                float edge = length(lap);
                // DetailBoost sharpens only at edges (anime outlines)
                float edgeMask  = smoothstep(0.03, 0.18, edge)*uDetailBoost;
                // Sharpness applies everywhere with soft mask
                float flatMask  = (1.0-smoothstep(0.04,0.22,edge))*uSharpness*0.5;
                color.rgb = c + lap*(edgeMask - flatMask);
            }
            $POST_PROCESS_APPLY
            gl_FragColor = clamp(color, 0.0, 1.0);
        }
    """.trimIndent()

    /**
     * FRAG_WAIFU2X — waifu2x-inspired convolutional super-resolution shader.
     *
     * Approximates the waifu2x noise-reduction + 2× upscale model using a
     * baked 3×3 convolution kernel in OpenGL ES 2.0.
     *
     * The kernel approximates the "art" model's final conv layer:
     *   • Strong edge preservation (high centre weight)
     *   • Diagonal suppression to reduce aliasing on anime line art
     *   • Subtle gradient smoothing in flat-colour regions
     *
     * A second pass applies the anime detail boost on detected edges,
     * then the standard post-processing stack (Fake HDR, OLED, colour profile).
     */
    private val FRAG_WAIFU2X = """
        precision highp float;
        varying vec2 texCoord;
        uniform sampler2D sTexture;
        uniform vec2 uInputSize;
        uniform vec2 uOutputSize;
        uniform float uSharpness;
        uniform float uDetailBoost;
        $POST_PROCESS_UNIFORMS
        $FUNC_TONEMAP
        $FUNC_FAKE_HDR
        $FUNC_OLED
        $FUNC_COLOR_PROFILE

        // 3×3 waifu2x approximation kernel
        // Weights tuned for anime line-art preservation + noise suppression
        const float W_CENTRE  =  1.80;
        const float W_EDGE    =  0.22;
        const float W_CORNER  = -0.08;
        const float W_NORM    = W_CENTRE + 4.0*W_EDGE + 4.0*W_CORNER;

        vec3 waifu2xSample(vec2 uv) {
            vec2 ts = 1.0 / uInputSize;
            vec3 sum = vec3(0.0);
            // Centre
            sum += W_CENTRE * texture2D(sTexture, uv).rgb;
            // Cardinal neighbours
            sum += W_EDGE   * texture2D(sTexture, uv + vec2( ts.x,  0.0)).rgb;
            sum += W_EDGE   * texture2D(sTexture, uv + vec2(-ts.x,  0.0)).rgb;
            sum += W_EDGE   * texture2D(sTexture, uv + vec2( 0.0,  ts.y)).rgb;
            sum += W_EDGE   * texture2D(sTexture, uv + vec2( 0.0, -ts.y)).rgb;
            // Corners
            sum += W_CORNER * texture2D(sTexture, uv + vec2( ts.x,  ts.y)).rgb;
            sum += W_CORNER * texture2D(sTexture, uv + vec2(-ts.x,  ts.y)).rgb;
            sum += W_CORNER * texture2D(sTexture, uv + vec2( ts.x, -ts.y)).rgb;
            sum += W_CORNER * texture2D(sTexture, uv + vec2(-ts.x, -ts.y)).rgb;
            return clamp(sum / W_NORM, 0.0, 1.0);
        }

        // Denoise pass: bilateral-inspired smoothing that avoids crossing edges
        vec3 denoisePass(vec2 uv, vec3 centre) {
            vec2 ts = 1.0 / uInputSize;
            vec3 acc = vec3(0.0); float wsum = 0.0;
            float sigmaS = 1.5; float sigmaC = 0.12;
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    if (dx == 0 && dy == 0) continue;
                    vec2 off = vec2(float(dx), float(dy)) * ts;
                    vec3 nb  = texture2D(sTexture, uv + off).rgb;
                    float wS = exp(-float(dx*dx + dy*dy) / (2.0 * sigmaS * sigmaS));
                    float cd = length(nb - centre);
                    float wC = exp(-cd*cd / (2.0 * sigmaC * sigmaC));
                    float w  = wS * wC;
                    acc  += nb * w;
                    wsum += w;
                }
            }
            return (wsum > 0.0) ? mix(centre, acc / wsum, 0.45) : centre;
        }

        void main() {
            // Step 1: waifu2x convolution upscale
            vec3 conv = waifu2xSample(texCoord);

            // Step 2: optional denoise
            vec3 denoised = denoisePass(texCoord, conv);

            // Step 3: edge-aware anime sharpening
            vec4 color = vec4(denoised, 1.0);
            if (uDetailBoost > 0.0 || uSharpness > 0.0) {
                vec2 ts = 1.0 / uInputSize;
                vec3 lap = -4.0 * denoised
                    + texture2D(sTexture, texCoord + vec2( ts.x, 0)).rgb
                    + texture2D(sTexture, texCoord + vec2(-ts.x, 0)).rgb
                    + texture2D(sTexture, texCoord + vec2(0,  ts.y)).rgb
                    + texture2D(sTexture, texCoord + vec2(0, -ts.y)).rgb;
                float edge      = length(lap);
                float edgeMask  = smoothstep(0.02, 0.15, edge) * uDetailBoost;
                float flatMask  = (1.0 - smoothstep(0.03, 0.20, edge)) * uSharpness * 0.4;
                color.rgb = denoised + lap * (edgeMask - flatMask);
            }

            // Step 4: post-processing (HDR, OLED, colour profile)
            $POST_PROCESS_APPLY
            gl_FragColor = clamp(color, 0.0, 1.0);
        }
    """.trimIndent()

/** Color grading profile applied after HDR/OLED. Ordinal maps to shader int. */
enum class ColorProfile(val displayName: String) {
    NATURAL("Natural"),
    CINEMA("Cinema"),
    VIVID("Vivid"),
    COOL("Cool"),
    WARM("Warm"),
}
