import { createFileRoute } from "@tanstack/react-router";
import { json, preflight, requireUser, getDB, getKV, checkRateLimit } from "@/lib/kuro-api";

export const Route = createFileRoute("/api/public/v1/sync")({
  server: {
    handlers: {
      OPTIONS: async () => preflight(),
      GET: async ({ request, env }: any) => {
        const auth = await requireUser(request, env);
        if ("error" in auth) return auth.error;

        const kv = getKV(env);
        const rate = await checkRateLimit(kv, `sync:${auth.userId}`, 120, 60);
        if (!rate.allowed) {
          return json({ error: "rate_limited", reset: rate.reset }, 429);
        }

        const db = getDB(env);
        const since = new URL(request.url).searchParams.get("since") || "0";

        const [favorites, watchHistory, settings, profiles] = await Promise.all([
          db.prepare(
            "SELECT * FROM sync_favorites WHERE user_id = ? AND updated_at > datetime(?, 'unixepoch') ORDER BY updated_at DESC"
          ).bind(auth.userId, since).all(),
          db.prepare(
            "SELECT * FROM sync_watch_history WHERE user_id = ? AND updated_at > datetime(?, 'unixepoch') ORDER BY last_watched_at DESC"
          ).bind(auth.userId, since).all(),
          db.prepare(
            "SELECT * FROM sync_settings WHERE user_id = ? AND updated_at > datetime(?, 'unixepoch')"
          ).bind(auth.userId, since).all(),
          db.prepare(
            "SELECT * FROM sync_profiles WHERE user_id = ? AND updated_at > datetime(?, 'unixepoch')"
          ).bind(auth.userId, since).all(),
        ]);

        return json({
          favorites: favorites.results ?? [],
          watchHistory: watchHistory.results ?? [],
          settings: settings.results ?? [],
          profiles: profiles.results ?? [],
          rate_limit: { remaining: rate.remaining, reset: rate.reset },
        });
      },
      PUT: async ({ request, env }: any) => {
        const auth = await requireUser(request, env);
        if ("error" in auth) return auth.error;

        const kv = getKV(env);
        const rate = await checkRateLimit(kv, `sync:write:${auth.userId}`, 60, 60);
        if (!rate.allowed) {
          return json({ error: "rate_limited", reset: rate.reset }, 429);
        }

        const body = await request.json().catch(() => null);
        if (!body || typeof body !== "object") {
          return json({ error: "invalid_body" }, 400);
        }

        const db = getDB(env);
        const now = new Date().toISOString();

        const results: Record<string, number> = {};

        if (body.favorites && Array.isArray(body.favorites)) {
          for (const fav of body.favorites) {
            await db.prepare(
              "INSERT OR REPLACE INTO sync_favorites (user_id, media_id, media_type, title, poster_url, updated_at) VALUES (?, ?, ?, ?, ?, ?)"
            ).bind(auth.userId, fav.media_id, fav.media_type || "anime", fav.title || null, fav.poster_url || null, now).run();
            results.favorites = (results.favorites || 0) + 1;
          }
        }

        if (body.watchHistory && Array.isArray(body.watchHistory)) {
          for (const entry of body.watchHistory) {
            await db.prepare(
              "INSERT OR REPLACE INTO sync_watch_history (user_id, media_id, episode_id, title, progress_ms, duration_ms, completed, last_watched_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)"
            ).bind(
              auth.userId,
              entry.media_id,
              entry.episode_id || null,
              entry.title || null,
              entry.progress_ms || 0,
              entry.duration_ms || 0,
              entry.completed ? 1 : 0,
              now
            ).run();
            results.watchHistory = (results.watchHistory || 0) + 1;
          }
        }

        if (body.settings && Array.isArray(body.settings)) {
          for (const setting of body.settings) {
            await db.prepare(
              "INSERT OR REPLACE INTO sync_settings (user_id, key, value, updated_at) VALUES (?, ?, ?, ?)"
            ).bind(auth.userId, setting.key, setting.value, now).run();
            results.settings = (results.settings || 0) + 1;
          }
        }

        if (body.profiles && Array.isArray(body.profiles)) {
          for (const profile of body.profiles) {
            await db.prepare(
              "INSERT OR REPLACE INTO sync_profiles (user_id, profile_id, name, avatar_url, is_kids, settings, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?)"
            ).bind(
              auth.userId,
              profile.profile_id,
              profile.name,
              profile.avatar_url || null,
              profile.is_kids ? 1 : 0,
              profile.settings ? JSON.stringify(profile.settings) : null,
              now
            ).run();
            results.profiles = (results.profiles || 0) + 1;
          }
        }

        return json({ synced: results, rate_limit: { remaining: rate.remaining, reset: rate.reset } });
      },
    },
  },
});
