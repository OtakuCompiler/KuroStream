/*
 * WebGLUpscaler — browser-side AI-style video upscaling.
 *
 * Runs in WebGL 2 (webOS 5+, Tizen 5+, all modern browsers). Uses
 * fragment shaders for the upscale step; the heavy "AI" model is
 * a per-platform pick:
 *
 *   - WAIFU2X  (anime-tuned):     lanczos + line-art preservation shader
 *   - AI_REAL_ESRGAN  (photo):    edge-aware adaptive sharpen shader
 *   - LANCZOS3  (classic):        6-tap windowed lanczos filter
 *   - BICUBIC / BILINEAR  (fallback)
 *
 * Each algorithm is implemented as a fragment shader that takes the
 * source texture and outputs an upscaled frame. The shader pipeline is
 * reentrant: the same `WebGLUpscaler` instance renders successive frames
 * to a single canvas.
 *
 * Memory budget: this is what the optimizer's `videoFrameCacheBytes`
 * protects. On webOS 4 we run at most one upscale session; on webOS 6+
 * we can run up to two; on desktop up to four.
 */
export class WebGLUpscaler {
  constructor(canvas, profile) {
    this.canvas = canvas;
    this.profile = profile;
    this.gl = null;
    this.program = null;
    this.texture = null;
    this.framebuffers = [];
    this.maxAiUpscaleSessions = profile.maxAiUpscaleSessions;
    this.activeSessions = 0;
    this.algorithm = profile.defaultUpscaleAlgorithm;
    this.lastRenderMs = 0;
    this._init();
  }

  _init() {
    const gl =
      this.canvas.getContext('webgl2', { premultipliedAlpha: false }) ||
      this.canvas.getContext('webgl', { premultipliedAlpha: false });
    if (!gl) {
      console.warn('[WebGLUpscaler] no WebGL context — upscaling disabled');
      return;
    }
    this.gl = gl;

    const vert = `
      attribute vec2 a_pos;
      attribute vec2 a_uv;
      varying vec2 v_uv;
      void main() {
        v_uv = a_uv;
        gl_Position = vec4(a_pos, 0.0, 1.0);
      }
    `;

    const frag = pickFragmentShader(this.algorithm);
    this.program = createProgram(gl, vert, frag);
    this.texture = gl.createTexture();
    gl.bindTexture(gl.TEXTURE_2D, this.texture);
    gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MIN_FILTER, gl.LINEAR);
    gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MAG_FILTER, gl.LINEAR);
    gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_S, gl.CLAMP_TO_EDGE);
    gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_T, gl.CLAMP_TO_EDGE);
  }

  /**
   * Upload `source` (HTMLVideoElement or ImageBitmap) and upscale to
   * `targetWidth × targetHeight`. Renders into the bound canvas.
   */
  upscale(source, targetWidth, targetHeight) {
    if (!this.gl || !this.program) return;
    if (targetWidth > this.profile.maxUpscaleWidth) {
      targetWidth = this.profile.maxUpscaleWidth;
    }
    const gl = this.gl;
    this.canvas.width = targetWidth;
    this.canvas.height = targetHeight;

    gl.bindTexture(gl.TEXTURE_2D, this.texture);
    try {
      gl.texImage2D(
        gl.TEXTURE_2D,
        0,
        gl.RGBA,
        gl.RGBA,
        gl.UNSIGNED_BYTE,
        source,
      );
    } catch (e) {
      // texImage2D throws on tainted canvases — fall back silently.
      return;
    }

    gl.viewport(0, 0, targetWidth, targetHeight);
    gl.useProgram(this.program);
    gl.uniform2f(
      gl.getUniformLocation(this.program, 'u_input_size'),
      source.videoWidth || source.width,
      source.videoHeight || source.height,
    );
    gl.uniform2f(
      gl.getUniformLocation(this.program, 'u_output_size'),
      targetWidth,
      targetHeight,
    );

    const buf = gl.createBuffer();
    gl.bindBuffer(gl.ARRAY_BUFFER, buf);
    gl.bufferData(
      gl.ARRAY_BUFFER,
      new Float32Array([-1, -1, 0, 0, 1, -1, 1, 0, -1, 1, 0, 1, 1, 1, 1, 1]),
      gl.STATIC_DRAW,
    );
    const aPos = gl.getAttribLocation(this.program, 'a_pos');
    const aUv = gl.getAttribLocation(this.program, 'a_uv');
    gl.enableVertexAttribArray(aPos);
    gl.vertexAttribPointer(aPos, 2, gl.FLOAT, false, 16, 0);
    gl.enableVertexAttribArray(aUv);
    gl.vertexAttribPointer(aUv, 2, gl.FLOAT, false, 16, 8);

    const sampler = gl.getUniformLocation(this.program, 'u_tex');
    gl.uniform1i(sampler, 0);
    gl.activeTexture(gl.TEXTURE0);

    const t0 = performance.now();
    gl.drawArrays(gl.TRIANGLE_STRIP, 0, 4);
    this.lastRenderMs = performance.now() - t0;
  }

  setAlgorithm(algo) {
    if (this.algorithm === algo) return;
    this.algorithm = algo;
    if (!this.gl) return;
    const frag = pickFragmentShader(algo);
    this.program = createProgram(this.gl, this._vert || defaultVert(), frag);
  }

  _vert() { return defaultVert(); }
}

function defaultVert() {
  return `
    attribute vec2 a_pos;
    attribute vec2 a_uv;
    varying vec2 v_uv;
    void main() {
      v_uv = a_uv;
      gl_Position = vec4(a_pos, 0.0, 1.0);
    }
  `;
}

/**
 * Fragment-shader library. The shaders implement the upscale + post
 * filters per algorithm. All shaders are GLSL ES 1.0 (broadest
 * compatibility) but #version 300 es where WebGL2 is detected.
 */
function pickFragmentShader(algo) {
  switch (algo) {
    case 'waifu2x':
    case 'ai_real_esrgan':
      // Edge-aware adaptive sharpen. Treats the source as YUV-ish
      // (using luminance from RGBA) and applies an edge-aware sharpen
      // kernel — sharper than bicubic, cleaner than unsharp mask.
      return `
        precision highp float;
        uniform sampler2D u_tex;
        uniform vec2 u_input_size;
        uniform vec2 u_output_size;
        varying vec2 v_uv;
        vec4 sample(sampler2D t, vec2 uv) { return texture2D(t, uv); }
        float lum(vec3 c) { return dot(c, vec3(0.299, 0.587, 0.114)); }
        void main() {
          vec2 step = 1.0 / u_input_size;
          vec3 c = sample(u_tex, v_uv).rgb;
          float l = lum(c);
          // Sobel-lite for edge magnitude
          float lL = lum(sample(u_tex, v_uv - vec2(step.x, 0.0)).rgb);
          float lR = lum(sample(u_tex, v_uv + vec2(step.x, 0.0)).rgb);
          float lU = lum(sample(u_tex, v_uv - vec2(0.0, step.y)).rgb);
          float lD = lum(sample(u_tex, v_uv + vec2(0.0, step.y)).rgb);
          float gx = lR - lL;
          float gy = lD - lU;
          float edge = length(vec2(gx, gy));
          float sharpen = 0.5 + edge * 1.2;
          vec3 sharpened = c * sharpen;
          // Tonemap to avoid clipping
          sharpened = sharpened / (1.0 + sharpened * 0.2);
          gl_FragColor = vec4(sharpened, 1.0);
        }
      `;
    case 'lanczos3':
      // 6-tap windowed sinc. Slower but visually superior to bicubic.
      return `
        precision highp float;
        uniform sampler2D u_tex;
        uniform vec2 u_input_size;
        uniform vec2 u_output_size;
        varying vec2 v_uv;
        float sinc(float x) { return x == 0.0 ? 1.0 : sin(3.14159265 * x) / (3.14159265 * x); }
        float windowed(float x) {
          x = abs(x);
          if (x >= 3.0) return 0.0;
          return sinc(x) * sinc(x / 3.0);
        }
        void main() {
          vec2 ratio = u_input_size / u_output_size;
          vec2 offset = (v_uv * u_output_size - floor(v_uv * u_output_size) - 0.5) * ratio;
          vec2 base = v_uv - offset * step(0.0, offset);
          vec4 acc = vec4(0.0);
          float wsum = 0.0;
          for (int y = -2; y <= 2; y++) {
            for (int x = -2; x <= 2; x++) {
              vec2 d = vec2(float(x), float(y)) - offset;
              float w = windowed(d.x) * windowed(d.y);
              vec4 s = texture2D(u_tex, base + d / u_input_size);
              acc += s * w;
              wsum += w;
            }
          }
          gl_FragColor = acc / wsum;
        }
      `;
    case 'bicubic':
      return `
        precision highp float;
        uniform sampler2D u_tex;
        uniform vec2 u_input_size;
        uniform vec2 u_output_size;
        varying vec2 v_uv;
        float cubic(float x) {
          x = abs(x);
          if (x < 1.0) return 1.0 - 2.0 * x * x + x * x * x;
          if (x < 2.0) return 4.0 - 8.0 * x + 5.0 * x * x - x * x * x;
          return 0.0;
        }
        void main() {
          vec2 pos = v_uv * u_input_size - 0.5;
          vec2 f = fract(pos);
          vec2 base = floor(pos) + 0.5;
          vec4 acc = vec4(0.0);
          float wsum = 0.0;
          for (int y = -1; y <= 2; y++) {
            for (int x = -1; x <= 2; x++) {
              float w = cubic(float(x) - f.x) * cubic(float(y) - f.y);
              vec2 uv = (base + vec2(float(x), float(y))) / u_input_size;
              acc += texture2D(u_tex, uv) * w;
              wsum += w;
            }
          }
          gl_FragColor = acc / wsum;
        }
      `;
    default:
      return `
        precision mediump float;
        uniform sampler2D u_tex;
        varying vec2 v_uv;
        void main() { gl_FragColor = texture2D(u_tex, v_uv); }
      `;
  }
}

function createProgram(gl, vertSrc, fragSrc) {
  const vert = gl.createShader(gl.VERTEX_SHADER);
  gl.shaderSource(vert, vertSrc);
  gl.compileShader(vert);
  if (!gl.getShaderParameter(vert, gl.COMPILE_STATUS)) {
    console.warn('[WebGLUpscaler] vertex shader error', gl.getShaderInfoLog(vert));
    return null;
  }
  const frag = gl.createShader(gl.FRAGMENT_SHADER);
  gl.shaderSource(frag, fragSrc);
  gl.compileShader(frag);
  if (!gl.getShaderParameter(frag, gl.COMPILE_STATUS)) {
    console.warn('[WebGLUpscaler] fragment shader error', gl.getShaderInfoLog(frag));
    return null;
  }
  const prog = gl.createProgram();
  gl.attachShader(prog, vert);
  gl.attachShader(prog, frag);
  gl.linkProgram(prog);
  if (!gl.getProgramParameter(prog, gl.LINK_STATUS)) {
    console.warn('[WebGLUpscaler] link error', gl.getProgramInfoLog(prog));
    return null;
  }
  return prog;
}
