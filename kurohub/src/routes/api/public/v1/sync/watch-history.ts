import { createFileRoute } from "@tanstack/react-router";
import { json, preflight, requireUser, getDB, getKV, checkRateLimit } from "@/lib/kuro-api";

export const Route = createFileRoute("/api/public/v1/sync/watch-history")({
  server: {
    handlers: {
      OPTIONS: async () => preflight(),
      GET: async ({ request, env }: any) => {
        const auth = await requireUser(request, env);
        if ("error" in auth) return auth.error;

        const db = getDB(env);
        const result = await db.prepare(
          "SELECT * FROM sync_watch_history WHERE user_id = ? ORDER BY last_watched_at DESC LIMIT 100"
        ).bind(auth.userId).all();

        return json({ watchHistory: result.results ?? [] });
      },
      POST: async ({ request, env }: any) => {
        const auth = await requireUser(request, env);
        if ("error" in auth) return auth.error;

        const body = await request.json().catch(() => null);
        if (!body?.media_id) return json({ error: "media_id_required" }, 400);

        const db = getDB(env);
        const now = new Date().toISOString();

        await db.prepare(
          "INSERT OR REPLACE INTO sync_watch_history (user_id, media_id, episode_id, title, progress_ms, duration_ms, completed, last_watched_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)"
        ).bind(
          auth.userId,
          body.media_id,
          body.episode_id || null,
          body.title || null,
          body.progress_ms || 0,
          body.duration_ms || 0,
          body.completed ? 1 : 0,
          now
        ).run();

        return json({ success: true });
      },
    },
  },
});
