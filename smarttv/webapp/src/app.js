/*
 * app.js — main entry point for the webOS / Tizen / generic web client.
 *
 * Boots in this order:
 *   1. Detect platform (webOS 4/5/6, Tizen 4/5/6, etc.)
 *   2. Load the matching PlatformProfile (caps + codec policy)
 *   3. Construct the PlatformMemoryOptimizer (RAM watchdog)
 *   4. Wire the resolver filter + MsePlayer + WebGLUpscaler
 *   5. Mount the Arctic Fuse 3-style UI shell
 *
 * The UI shell is intentionally minimal here — the real rendering uses
 * the same Compose / web components as the Android app. This entry
 * focuses on the platform pipeline that's specific to webOS/Tizen.
 */
import { detectPlatformKind, detectWebOSVersion, detectTizenVersion } from './platform.js';
import { profileFor, PlatformProfile } from './profiles.js';
import { PlatformMemoryOptimizer, BoundedCache } from './memory-optimizer.js';
import { MsePlayer } from './mse-player.js';
import { WebGLUpscaler } from './webgl-upscaler.js';

const state = {
  profile: null,
  optimizer: null,
  player: null,
  upscaler: null,
  caches: [],
};

const dom = {
  status: document.getElementById('status'),
  details: document.getElementById('details'),
  search: document.getElementById('search'),
  results: document.getElementById('results'),
  canvas: document.getElementById('upscale-canvas'),
  video: document.getElementById('player-video'),
};

boot().catch((e) => {
  console.error('[kuro] boot failed', e);
  if (dom.status) dom.status.textContent = 'Init failed: ' + e.message;
});

async function boot() {
  setStatus('Detecting platform…');

  const kind = detectPlatformKind();
  let version = null;
  if (kind === 'webos_tv') version = detectWebOSVersion();
  else if (kind === 'tizen_tv') version = detectTizenVersion();

  const profile = profileFor(kind, version);
  state.profile = profile;
  setStatus(`Running on ${profile.displayLabel}`);
  if (dom.details) {
    dom.details.textContent = JSON.stringify({
      kind: profile.kind,
      displayLabel: profile.displayLabel,
      ramBudgetMb: profile.ramBudgetMb,
      supports4kDecode: profile.supports4kDecode,
      supportsDolbyAtmosPassthrough: profile.supportsDolbyAtmosPassthrough,
      defaultUpscaleAlgorithm: profile.defaultUpscaleAlgorithm,
      maxUpscaleWidth: profile.maxUpscaleWidth,
      codecFallback: profile.codecFallback,
    }, null, 2);
  }

  // Memory optimizer: registers bounded caches so the working set stays
  // inside `profile.ramBudgetMb`.
  state.optimizer = new PlatformMemoryOptimizer(profile, browserMemoryProbe());
  state.optimizer.onPressure(({ from, to, mb }) => {
    console.warn(`[kuro] memory pressure ${from} → ${to} at ${mb}MB`);
    setStatus(`Memory pressure: ${to} (${mb}MB / ${profile.ramBudgetMb}MB)`);
  });
  state.optimizer.start();

  // Register the catalog & search caches with the optimizer.
  const catalogCache = new BoundedCache('catalog', 'catalog', 4 * 1024 * 1024);
  const posterCache = new BoundedCache('posters', 'image', 8 * 1024 * 1024);
  state.caches.push(catalogCache, posterCache);
  state.optimizer.register(catalogCache);
  state.optimizer.register(posterCache);

  // WebGL upscaler (only if we actually support it on this profile).
  if (dom.canvas) {
    state.upscaler = new WebGLUpscaler(dom.canvas, profile);
  }

  // MSE player (only if we have a video element to play on).
  if (dom.video) {
    state.player = new MsePlayer(profile, dom.video, state.optimizer);
  }

  // Wire search.
  if (dom.search) {
    dom.search.addEventListener('input', (e) => {
      const q = String(e.target.value || '').trim();
      if (!q) return;
      // Real impl: state.search.search(q).collect { ... }
      // The UI below is a stub that documents what the resolver would
      // surface after the codec matrix filters incompatible streams.
      renderSearchStub(q);
    });
  }

  // Wire keyboard for TV remotes.
  wireRemoteKeys();

  setStatus('Ready');
}

function setStatus(s) {
  if (dom.status) dom.status.textContent = s;
}

function browserMemoryProbe() {
  // performance.memory is the cheapest available probe on Chromium-based
  // webOS 5+/Tizen 5+. On webOS 4 (no Chromium) we return 0 — the
  // optimizer treats NOMINAL state and waits for the sourceBuffer error
  // event to fire if memory really is tight.
  if (typeof performance !== 'undefined' && performance.memory) {
    return () => performance.memory.usedJSHeapSize || 0;
  }
  return () => 0;
}

function renderSearchStub(query) {
  if (!dom.results) return;
  dom.results.innerHTML = '';
  const li = document.createElement('li');
  li.textContent = `Search for "${query}" — resolver runs ${state.profile.codecFallback} fallback`;
  dom.results.appendChild(li);
}

function wireRemoteKeys() {
  const KEY_MAP = {
    37: 'left', 38: 'up', 39: 'right', 40: 'down',
    13: 'enter', 0: 'enter',
    415: 'play', 417: 'forward', 412: 'back', 19: 'pause',
    403: 'red', 404: 'green', 405: 'yellow', 406: 'blue',
  };
  document.addEventListener('keydown', (e) => {
    const k = KEY_MAP[e.keyCode];
    if (!k) return;
    if (k === 'enter') {
      const active = document.activeElement;
      if (active && active.tagName === 'INPUT') {
        // Let the input handle Enter.
        return;
      }
      // Otherwise treat Enter on TV remote as "open search".
      if (dom.search) dom.search.focus();
    }
  });
}
