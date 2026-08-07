/*
 * MsePlayer — MediaSource Extensions-based player for webOS / Tizen.
 *
 * The webOS 4/5/6 native MediaSource decoder chokes on HEVC 10-bit,
 * HDR10+ metadata, Dolby Vision, DTS-HD audio, and multi-audio-track
 * streams. This class wraps MSE with the platform codec matrix so the
 * player NEVER sees an incompatible stream — incompatible streams are
 * either:
 *   - rejected at resolve-time (filter_at_source strategy), or
 *   - transcoded into H.264 + AAC before being fed to MSE.
 *
 * The actual transcoding is delegated to a worker that hosts an
 * FFmpeg.wasm build; see `ffmpeg-wasm/`. The worker talks back via
 * `postMessage({ type: 'ready', url })` once the transcode is up.
 *
 * If even transcode won't help (e.g. webOS 4 + AV1 where memory
 * budget won't allow the wasm runtime), the resolver skips the
 * stream and asks the addon for another.
 */
export class MsePlayer {
  constructor(profile, videoElement, memoryOptimizer) {
    this.profile = profile;
    this.video = videoElement;
    this.optimizer = memoryOptimizer;
    this.mediaSource = null;
    this.sourceBuffer = null;
    this.worker = null;
    this.ready = false;
    this.queue = [];
    this.bytesAppended = 0;
    this.lastError = null;
  }

  /**
   * Start playback. `probe` describes the candidate stream so we can
   * decide direct-play vs transcode; `sourceUrl` is the actual media
   * URL the player should consume.
   *
   * Returns a promise that resolves when playback starts.
   */
  async play(probe, sourceUrl) {
    const verdict = check(probe);
    if (!verdict.canPlay) {
      throw new MseError('unsupported', verdict.reasons.join('; '));
    }

    if (verdict.needsTranscode) {
      return this.playViaTranscode(probe, sourceUrl, verdict.transcodeRequired);
    }
    return this.playDirect(probe, sourceUrl);
  }

  async playDirect(probe, sourceUrl) {
    if (!('MediaSource' in window)) {
      throw new MseError('no_mse', 'MediaSource not supported on this browser');
    }
    this.mediaSource = new MediaSource();
    this.video.src = URL.createObjectURL(this.mediaSource);

    return new Promise((resolve, reject) => {
      this.mediaSource.addEventListener('sourceopen', () => {
        try {
          const mime = pickMimeFor(probe);
          this.sourceBuffer = this.mediaSource.addSourceBuffer(mime);
          this.sourceBuffer.mode = 'segments';
          this.sourceBuffer.addEventListener('updateend', () => this._drainQueue());
          this.sourceBuffer.addEventListener('error', (e) => {
            this.lastError = 'source_buffer_error';
            reject(new MseError('source_buffer_error', String(e)));
          });
          this.ready = true;
          // Append the source.
          fetch(sourceUrl, { credentials: 'omit' })
            .then((r) => r.arrayBuffer())
            .then((buf) => {
              if (this.sourceBuffer.updating || this.queue.length > 0) {
                this.queue.push(buf);
                this._drainQueue();
              } else {
                this.sourceBuffer.appendBuffer(buf);
                this.bytesAppended += buf.byteLength;
              }
              this.video.play().then(resolve).catch(reject);
            })
            .catch((e) => reject(new MseError('fetch_error', String(e))));
        } catch (e) {
          reject(new MseError('mse_init_error', String(e)));
        }
      });
    });
  }

  async playViaTranscode(probe, sourceUrl, requirements) {
    if (!this.profile.codecFallback.includes('transcode')) {
      throw new MseError('transcode_required_but_disabled',
        'Profile requires transcode but codecFallback=' + this.profile.codecFallback);
    }

    // Spin up the FFmpeg.wasm worker. The worker transcodes the source
    // URL on-the-fly into H.264 + AAC and posts back the resulting blob
    // URL via `ready`.
    const workerScript = `${location.origin}/ffmpeg-wasm/worker.js`;
    return new Promise((resolve, reject) => {
      this.worker = new Worker(workerScript);
      const timeout = setTimeout(() => {
        reject(new MseError('transcode_timeout',
          `FFmpeg.wasm transcode did not start within ${30_000}ms`));
      }, 30_000);

      this.worker.addEventListener('message', (ev) => {
        const { type, url, error } = ev.data || {};
        if (type === 'ready') {
          clearTimeout(timeout);
          this.playDirect(probe, url).then(resolve).catch(reject);
        } else if (type === 'error') {
          clearTimeout(timeout);
          reject(new MseError('transcode_error', error || 'unknown'));
        } else if (type === 'progress') {
          this.lastProgress = ev.data;
        }
      });

      this.worker.postMessage({
        type: 'start',
        sourceUrl,
        requirements,
        profile: {
          ramBudgetMb: this.profile.ramBudgetMb,
          maxUpscaleWidth: this.profile.maxUpscaleWidth,
        },
      });
    });
  }

  _drainQueue() {
    if (!this.sourceBuffer || this.sourceBuffer.updating) return;
    if (this.queue.length === 0) return;
    const next = this.queue.shift();
    try {
      this.sourceBuffer.appendBuffer(next);
      this.bytesAppended += next.byteLength;
    } catch (e) {
      this.lastError = 'append_error';
      throw e;
    }
  }

  stop() {
    if (this.worker) {
      this.worker.postMessage({ type: 'stop' });
      this.worker.terminate();
      this.worker = null;
    }
    if (this.video) {
      this.video.pause();
      this.video.removeAttribute('src');
      this.video.load();
    }
    if (this.mediaSource) {
      if (this.mediaSource.readyState === 'open') this.mediaSource.endOfStream();
      this.mediaSource = null;
    }
    this.sourceBuffer = null;
    this.ready = false;
    this.queue = [];
  }

  snapshot() {
    return {
      ready: this.ready,
      bytesAppended: this.bytesAppended,
      queueDepth: this.queue.length,
      lastError: this.lastError,
      profile: this.profile.displayLabel,
    };
  }
}

export class MseError extends Error {
  constructor(code, detail) {
    super(`${code}: ${detail}`);
    this.code = code;
    this.detail = detail;
  }
}

function pickMimeFor(probe) {
  // Pick the most-compatible MSE MIME the host supports. The browser's
  // own isTypeSupported() is authoritative — webOS 4 typically supports
  // only H.264/AAC in MP4; webOS 5/6 add HEVC and WebM; Tizen has its
  // own list. We probe at runtime and fall back.
  const candidates = [
    'video/mp4; codecs="avc1.640028,mp4a.40.2"',     // H.264 High + AAC LC
    'video/mp4; codecs="avc1.42E01E,mp4a.40.2"',     // H.264 Baseline + AAC
    'video/webm; codecs="vp9,opus"',                  // VP9 + Opus
    'video/mp4; codecs="avc1.640028"',               // H.264 video only
    'video/webm', 'video/mp4',
  ];
  for (const mime of candidates) {
    if (window.MediaSource && MediaSource.isTypeSupported(mime)) return mime;
  }
  return candidates[0];
}

// Imported lazily to avoid circular deps.
import { check } from './codec-matrix.js';
