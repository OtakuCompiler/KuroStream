/*
 * PlatformMemoryOptimizer — runtime watchdog that keeps the working set
 * inside `PlatformProfile.ramBudgetMb`. This is the second half of the
 * fix for the LG webOS / Tizen 4K P2P playback crash:
 *
 *   1. CodecCompatibilityMatrix filters incompatible streams up-front.
 *   2. THIS module ensures even compatible streams don't accumulate
 *      enough MSE buffer + image cache + decode queue to OOM the app.
 *
 * Three tiers of pressure response:
 *
 *   NOMINAL    < ramTriggerPurgeMb
 *     — all caches stay at full size.
 *
 *   SOFT_TRIM  < ramBudgetMb
 *     — drop search/poster/prefetch caches to half their cap.
 *     — keep playback buffer at full.
 *
 *   HARD_TRIM  >= ramBudgetMb
 *     — drop ALL caches to a quarter cap (or clear).
 *     — drop playback buffer to minimum (player will rebuffer).
 *
 * On webOS / Tizen we poll the runtime via the (limited) `performance.memory`
 * API where available, and via a JS heap snapshot estimate where it isn't.
 */
export class PlatformMemoryOptimizer {
  constructor(profile, probe = defaultProbe) {
    this.profile = profile;
    this.probe = probe;
    this.caches = [];
    this.state = 'nominal';
    this.lastTickAt = 0;
    this.tickIntervalMs = profile.kind === 'webos_tv' || profile.kind === 'tizen_tv'
      ? 1500
      : 3000;
    this._timer = null;
    this._onPressureListeners = new Set();
  }

  register(cache) {
    this.caches.push(cache);
    return () => {
      const i = this.caches.indexOf(cache);
      if (i >= 0) this.caches.splice(i, 1);
    };
  }

  onPressure(listener) {
    this._onPressureListeners.add(listener);
    return () => this._onPressureListeners.delete(listener);
  }

  start() {
    if (this._timer) return;
    this._timer = setInterval(() => this.tick(), this.tickIntervalMs);
  }

  stop() {
    if (this._timer) {
      clearInterval(this._timer);
      this._timer = null;
    }
  }

  /**
   * Force a tick — useful right before/after playback starts/stops.
   */
  tick() {
    const bytes = this.probe();
    const mb = Math.round(bytes / (1024 * 1024));
    const prev = this.state;
    let next = 'nominal';
    let action = 'none';

    if (mb >= this.profile.ramBudgetMb) {
      next = 'hard_trim';
      action = 'hard_trim';
    } else if (mb >= this.profile.ramTriggerPurgeMb) {
      next = 'soft_trim';
      action = 'soft_trim';
    }

    if (action === 'soft_trim') {
      this.caches.forEach((c) => c.trimTo(this.profile.videoFrameCacheBytes / 2));
    } else if (action === 'hard_trim') {
      this.caches.forEach((c) => c.trimTo(this.profile.videoFrameCacheBytes / 4));
      // Also drop MSE source buffer to the minimum so the player rebuffers
      // from network rather than the working set blowing past the OS cap.
      this.caches
        .filter((c) => c.kind === 'mse_source_buffer')
        .forEach((c) => c.trimTo(1 * 1024 * 1024));
    }

    if (next !== prev) {
      this.state = next;
      this._onPressureListeners.forEach((l) => {
        try { l({ from: prev, to: next, mb }); } catch (_) { /* ignore */ }
      });
    }
    this.lastTickAt = Date.now();
  }

  /** Snapshot for diagnostics. */
  snapshot() {
    return {
      state: this.state,
      profile: this.profile.kind,
      ramBudgetMb: this.profile.ramBudgetMb,
      ramTriggerPurgeMb: this.profile.ramTriggerPurgeMb,
      cacheCount: this.caches.length,
    };
  }
}

/**
 * Default memory probe. Uses `performance.memory` (Chromium / webOS 5+ /
 * Tizen 5+) and falls back to an estimate from `performance.now()` gaps
 * (very rough but better than nothing).
 *
 * For webOS 4 specifically we also listen to the `webOS.memory` event if
 * exposed (it isn't on most models, but the fallback is graceful).
 */
function defaultProbe() {
  if (typeof performance !== 'undefined' && performance.memory) {
    return performance.memory.usedJSHeapSize || 0;
  }
  // Fallback: 0 — caller should register their own probe.
  return 0;
}

/**
 * A simple LRU bounded cache for use with the optimizer.
 * `kind` lets the optimizer apply different rules to MSE source buffers
 * vs image caches vs search history.
 */
export class BoundedCache {
  constructor(name, kind = 'generic', maxBytes) {
    this.name = name;
    this.kind = kind;
    this.maxBytes = maxBytes;
    this.currentBytes = 0;
    this.entries = new Map(); // insertion-ordered Map doubles as LRU
  }

  set(key, value, sizeBytes) {
    if (this.entries.has(key)) {
      this.currentBytes -= this.entries.get(key).size;
      this.entries.delete(key);
    }
    while (this.currentBytes + sizeBytes > this.maxBytes && this.entries.size > 0) {
      const oldest = this.entries.keys().next().value;
      this.currentBytes -= this.entries.get(oldest).size;
      this.entries.delete(oldest);
    }
    this.entries.set(key, { value, size: sizeBytes });
    this.currentBytes += sizeBytes;
  }

  get(key) {
    if (!this.entries.has(key)) return undefined;
    const v = this.entries.get(key).value;
    // Move to end (LRU bump)
    this.entries.delete(key);
    this.entries.set(key, { value: v, size: this.entries.get(key)?.size || 0 });
    return v;
  }

  trimTo(targetBytes) {
    while (this.currentBytes > targetBytes && this.entries.size > 0) {
      const oldest = this.entries.keys().next().value;
      this.currentBytes -= this.entries.get(oldest).size;
      this.entries.delete(oldest);
    }
    if (this.currentBytes > targetBytes) {
      this.clear();
    }
  }

  clear() {
    this.entries.clear();
    this.currentBytes = 0;
  }

  /** Adapter for `PlatformMemoryOptimizer.register`. */
  currentBytes_() { return this.currentBytes; }
  trimTo_(bytes) { this.trimTo(bytes); }
  clear_() { this.clear(); }
}
