class WebGLUpscaler {
  private canvas: HTMLCanvasElement
  private gl: WebGLRenderingContext | null = null
  private program: WebGLProgram | null = null
  private texture: WebGLTexture | null = null
  private animationId: number | null = null
  private video: HTMLVideoElement | null = null
  private videoWidth = 0
  private videoHeight = 0
  private targetWidth = 0
  private targetHeight = 0

  constructor(canvas: HTMLCanvasElement) {
    this.canvas = canvas
    const gl = canvas.getContext('webgl', { antialias: false, alpha: false })
    if (!gl) throw new Error('WebGL not supported')
    this.gl = gl
    this.initShaders()
    this.initBuffers()
  }

  private initShaders() {
    const gl = this.gl!
    const vs = `
      attribute vec2 aPosition;
      attribute vec2 aTexCoord;
      varying vec2 vTexCoord;
      void main() {
        gl_Position = vec4(aPosition, 0.0, 1.0);
        vTexCoord = aTexCoord;
      }
    `
    const fs = `
      precision mediump float;
      varying vec2 vTexCoord;
      uniform sampler2D uTexture;
      uniform vec2 uVideoSize;
      uniform float uSharpness;
      uniform float uContrast;
      uniform float uSaturation;
      uniform float uBrightness;

      vec4 cubicSample(sampler2D tex, vec2 uv, vec2 texSize) {
        vec2 texelSize = 1.0 / texSize;
        vec2 coord = uv * texSize - 0.5;
        vec2 f = fract(coord);
        vec2 i = floor(coord);

        vec4 result = vec4(0.0);
        float totalWeight = 0.0;

        for (int x = 0; x < 4; x++) {
          for (int y = 0; y < 4; y++) {
            vec2 offset = vec2(float(x) - 1.0, float(y) - 1.0);
            vec2 sampleCoord = (i + offset + 0.5) / texSize;
            vec2 d = abs(offset - f + 0.5);
            float wx = (d.x < 1.0) ? (0.5 * d.x * d.x * d.x - d.x * d.x + 2.0/3.0) : (0.5 * ((-d.x + 2.0) * (-d.x + 2.0) * (-d.x + 2.0) - 4.0 * ((-d.x + 2.0) * (-d.x + 2.0)) + 2.0));
            float wy = (d.y < 1.0) ? (0.5 * d.y * d.y * d.y - d.y * d.y + 2.0/3.0) : (0.5 * ((-d.y + 2.0) * (-d.y + 2.0) * (-d.y + 2.0) - 4.0 * ((-d.y + 2.0) * (-d.y + 2.0)) + 2.0));
            float weight = wx * wy;
            result += texture2D(tex, sampleCoord) * weight;
            totalWeight += weight;
          }
        }
        return result / max(totalWeight, 0.001);
      }

      void main() {
        vec4 color = cubicSample(uTexture, vTexCoord, uVideoSize);

        // Sharpening (unsharp mask)
        if (uSharpness > 0.0) {
          vec2 texelSize = 1.0 / uVideoSize;
          vec4 blur = vec4(0.0);
          blur += texture2D(uTexture, vTexCoord + vec2(-texelSize.x, 0.0));
          blur += texture2D(uTexture, vTexCoord + vec2(texelSize.x, 0.0));
          blur += texture2D(uTexture, vTexCoord + vec2(0.0, -texelSize.y));
          blur += texture2D(uTexture, vTexCoord + vec2(0.0, texelSize.y));
          blur *= 0.25;

          vec4 sharpened = color + (color - blur) * uSharpness * 2.0;
          color = mix(color, sharpened, min(uSharpness, 1.0));
        }

        // Color adjustments
        color.rgb = (color.rgb - 0.5) * uContrast + 0.5;
        color.rgb = color.rgb * uBrightness;

        // Saturation
        float gray = dot(color.rgb, vec3(0.2126, 0.7152, 0.0722));
        color.rgb = mix(vec3(gray), color.rgb, uSaturation);

        color.rgb = clamp(color.rgb, 0.0, 1.0);
        gl_FragColor = color;
      }
    `

    const vertShader = gl.createShader(gl.VERTEX_SHADER)!
    gl.shaderSource(vertShader, vs)
    gl.compileShader(vertShader)
    if (!gl.getShaderParameter(vertShader, gl.COMPILE_STATUS)) {
      console.error('Vertex shader error:', gl.getShaderInfoLog(vertShader))
    }

    const fragShader = gl.createShader(gl.FRAGMENT_SHADER)!
    gl.shaderSource(fragShader, fs)
    gl.compileShader(fragShader)
    if (!gl.getShaderParameter(fragShader, gl.COMPILE_STATUS)) {
      console.error('Fragment shader error:', gl.getShaderInfoLog(fragShader))
    }

    this.program = gl.createProgram()!
    gl.attachShader(this.program, vertShader)
    gl.attachShader(this.program, fragShader)
    gl.linkProgram(this.program)
    gl.useProgram(this.program)
  }

  private initBuffers() {
    const gl = this.gl!
    const positions = new Float32Array([
      -1, -1,  1, -1,  -1, 1,
      -1,  1,  1, -1,   1, 1,
    ])
    const texCoords = new Float32Array([
      0, 1,  1, 1,  0, 0,
      0, 0,  1, 1,  1, 0,
    ])

    const posBuffer = gl.createBuffer()
    gl.bindBuffer(gl.ARRAY_BUFFER, posBuffer)
    gl.bufferData(gl.ARRAY_BUFFER, positions, gl.STATIC_DRAW)
    const aPosition = gl.getAttribLocation(this.program!, 'aPosition')
    gl.enableVertexAttribArray(aPosition)
    gl.vertexAttribPointer(aPosition, 2, gl.FLOAT, false, 0, 0)

    const texBuffer = gl.createBuffer()
    gl.bindBuffer(gl.ARRAY_BUFFER, texBuffer)
    gl.bufferData(gl.ARRAY_BUFFER, texCoords, gl.STATIC_DRAW)
    const aTexCoord = gl.getAttribLocation(this.program!, 'aTexCoord')
    gl.enableVertexAttribArray(aTexCoord)
    gl.vertexAttribPointer(aTexCoord, 2, gl.FLOAT, false, 0, 0)
  }

  setTargetSize(width: number, height: number) {
    if (this.targetWidth !== width || this.targetHeight !== height) {
      this.targetWidth = width
      this.targetHeight = height
      this.canvas.width = width
      this.canvas.height = height
      this.gl?.viewport(0, 0, width, height)
    }
  }

  setVideo(video: HTMLVideoElement) {
    this.video = video
    const gl = this.gl!
    this.texture = gl.createTexture()
    gl.bindTexture(gl.TEXTURE_2D, this.texture)
    gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_S, gl.CLAMP_TO_EDGE)
    gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_T, gl.CLAMP_TO_EDGE)
    gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MIN_FILTER, gl.LINEAR)
    gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MAG_FILTER, gl.LINEAR)
  }

  render(sharpness: number, contrast: number, saturation: number, brightness: number) {
    if (!this.gl || !this.program || !this.video || !this.texture) return

    const gl = this.gl
    const video = this.video

    if (video.videoWidth !== this.videoWidth || video.videoHeight !== this.videoHeight) {
      this.videoWidth = video.videoWidth
      this.videoHeight = video.videoHeight
    }

    gl.bindTexture(gl.TEXTURE_2D, this.texture)
    gl.texImage2D(gl.TEXTURE_2D, 0, gl.RGBA, gl.RGBA, gl.UNSIGNED_BYTE, video)

    const uVideoSize = gl.getUniformLocation(this.program, 'uVideoSize')
    const uSharpness = gl.getUniformLocation(this.program, 'uSharpness')
    const uContrast = gl.getUniformLocation(this.program, 'uContrast')
    const uSaturation = gl.getUniformLocation(this.program, 'uSaturation')
    const uBrightness = gl.getUniformLocation(this.program, 'uBrightness')

    gl.uniform2f(uVideoSize, video.videoWidth, video.videoHeight)
    gl.uniform1f(uSharpness, sharpness / 100)
    gl.uniform1f(uContrast, contrast / 100)
    gl.uniform1f(uSaturation, saturation / 100)
    gl.uniform1f(uBrightness, brightness / 100)

    gl.drawArrays(gl.TRIANGLES, 0, 6)
  }

  start(callback: () => void) {
    const loop = () => {
      callback()
      this.animationId = requestAnimationFrame(loop)
    }
    loop()
  }

  stop() {
    if (this.animationId !== null) {
      cancelAnimationFrame(this.animationId)
      this.animationId = null
    }
  }

  destroy() {
    this.stop()
    if (this.gl && this.texture) {
      this.gl.deleteTexture(this.texture)
    }
  }
}

export function createWebGLUpscaler(canvas: HTMLCanvasElement) {
  return new WebGLUpscaler(canvas)
}
