/*
 * FFmpeg.wasm worker — on-the-fly stream transcode.
 *
 * Lives in its own Worker so the transcode never blocks the main thread
 * (which would freeze the UI and trigger the same OOM-on-jank symptoms
 * that cause the LG "this app will now restart to free up memory" prompt).
 *
 * Receives a start message from `MsePlayer.playViaTranscode`:
 *
 *   {
 *     type: 'start',
 *     sourceUrl: string,
 *     requirements: {
 *       transcodeVideo: boolean,
 *       transcodeAudio: boolean,
 *       targetVideoCodec: 'h264',
 *       targetAudioCodec: 'aac',
 *       targetAudioChannels: 2,
 *     },
 *     profile: { ramBudgetMb: number, maxUpscaleWidth: number },
 *   }
 *
 * Builds an FFmpeg.wasm invocation that:
 *   - Demuxes the source (mkv/mp4/ts/avi)
 *   - Maps the first video stream, transcoding to H.264 main profile 8-bit
 *     with a target bitrate derived from `profile.ramBudgetMb` (lower
 *     bitrate = smaller MSE buffer = less RAM).
 *   - Maps the first audio stream only (single-track) and transcodes to
 *     AAC LC stereo if the source is anything else (DTS, TrueHD, EAC3).
 *   - Drops subtitles (webOS handles embedded subs poorly).
 *   - Posts a 'ready' message with a Blob URL the main thread can feed
 *     into MSE.
 *
 * Memory cap: the wasm runtime size is tuned per profile. webOS 4 caps
 * the wasm heap to 96 MB; webOS 6+ allows 192 MB. If the worker OOMs
 * mid-transcode it posts 'error' with the message so the player can
 * fall back to the next stream.
 *
 * NOTE: the FFmpeg.wasm binary lives at /ffmpeg-wasm/ffmpeg-core.js
 * and is loaded lazily on first use. Browsers cap Worker CPU and
 * memory, so transcode rate is roughly 0.3-0.6× realtime on TV CPUs.
 */
let ffmpeg = null;
let cancelled = false;

self.addEventListener('message', async (ev) => {
  const { type } = ev.data || {};
  if (type === 'start') return handleStart(ev.data);
  if (type === 'stop') return handleStop();
});

async function handleStart(msg) {
  cancelled = false;
  try {
    const { sourceUrl, requirements, profile } = msg;
    const ffmpeg = await loadFfmpeg(profile);

    const args = buildArgs(sourceUrl, requirements, profile);
    self.postMessage({ type: 'progress', stage: 'starting' });

    // Run synchronously inside the worker; collect chunks and expose
    // as a Blob URL.
    const chunks = [];
    ffmpeg.setLogger(({ type: logType, message }) => {
      if (logType === 'fferr') {
        // Trim noise.
        const trimmed = String(message || '').slice(0, 200);
        self.postMessage({ type: 'log', line: trimmed });
      }
    });
    ffmpeg.setProgress(({ progress, time }) => {
      if (cancelled) return;
      self.postMessage({
        type: 'progress',
        progress: Math.min(100, Math.max(0, progress * 100)),
        time,
      });
    });

    const code = await ffmpeg.exec(args);
    if (cancelled) return;
    if (code !== 0) {
      self.postMessage({ type: 'error', error: `ffmpeg exit ${code}` });
      return;
    }

    const data = ffmpeg.FS('readFile', 'out.mp4');
    const blob = new Blob([data.buffer], { type: 'video/mp4' });
    const url = URL.createObjectURL(blob);
    self.postMessage({ type: 'ready', url });
  } catch (e) {
    self.postMessage({ type: 'error', error: String(e && e.message || e) });
  }
}

function handleStop() {
  cancelled = true;
  if (ffmpeg && typeof ffmpeg.exit === 'function') {
    try { ffmpeg.exit(); } catch (_) { /* ignore */ }
  }
}

async function loadFfmpeg(profile) {
  if (ffmpeg) return ffmpeg;
  importScripts(`${self.location.origin}/ffmpeg-wasm/ffmpeg-core.js`);
  // eslint-disable-next-line no-undef
  ffmpeg = await FFmpeg.createFFmpeg({
    corePath: `${self.location.origin}/ffmpeg-wasm/ffmpeg-core.wasm`,
    log: false,
    logger: () => {},
  });
  // Cap wasm heap by profile budget. webOS 4 = 96 MB, webOS 6+ = 192 MB,
  // desktop = no cap.
  const memCapMb = profile.ramBudgetMb < 350 ? 96
    : profile.ramBudgetMb < 500 ? 192
    : 1024;
  if (ffmpeg.setModuleArgs) {
    ffmpeg.setModuleArgs({ INITIAL_MEMORY: memCapMb * 1024 * 1024 });
  }
  await ffmpeg.load();
  return ffmpeg;
}

function buildArgs(sourceUrl, requirements, profile) {
  const args = [];

  // Network input via HTTP — FFmpeg.wasm needs special handling here;
  // in practice we pre-buffer the first N MB into MEMFS via fetch and
  // pass `input.mp4` as the file argument. To keep this skeleton
  // self-contained, we accept the URL and let FFmpeg fetch internally
  // (most builds support the `-i` http URL via concat).

  args.push('-nostdin', '-hide_banner', '-loglevel', 'error');
  // Single video track, drop subtitles.
  args.push('-map', '0:v:0', '-sn');

  // Audio: only the first track on webOS/Tizen.
  args.push('-map', '0:a:0?');

  if (requirements.transcodeVideo) {
    // Downscale any 4K input to fit profile.maxUpscaleWidth; cap bitrate
    // by ramBudgetMb so MSE buffer stays in budget.
    const width = Math.min(profile.maxUpscaleWidth, 3840);
    const bitrateK = profile.ramBudgetMb < 350 ? 6000
      : profile.ramBudgetMb < 500 ? 10000
      : 20000;
    args.push(
      '-vf', `scale='min(${width},iw)':-2`,
      '-c:v', 'libx264',
      '-profile:v', requirements.targetVideoProfile || 'high',
      '-pix_fmt', 'yuv420p',
      '-preset', profile.ramBudgetMb < 350 ? 'ultrafast' : 'veryfast',
      '-b:v', `${bitrateK}k`,
      '-maxrate', `${bitrateK * 1.2}k`,
      '-bufsize', `${bitrateK * 2}k`,
      '-g', '60',
    );
  } else {
    args.push('-c:v', 'copy');
  }

  if (requirements.transcodeAudio) {
    args.push(
      '-c:a', requirements.targetAudioCodec || 'aac',
      '-ac', String(requirements.targetAudioChannels || 2),
      '-ar', '48000',
      '-b:a', '192k',
    );
  } else {
    args.push('-c:a', 'copy');
  }

  // Output as fragmented MP4 for MSE.
  args.push(
    '-movflags', '+faststart+frag_keyframe+empty_moov',
    '-f', 'mp4',
    'out.mp4',
  );

  // Input.
  args.push('-i', sourceUrl);

  return args;
}
