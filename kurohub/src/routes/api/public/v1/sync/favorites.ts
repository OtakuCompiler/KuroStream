import { createFileRoute } from "@tanstack/react-router";
import { json, preflight, requireUser, getDB, getKV, checkRateLimit } from "@/lib/kuro-api";

export const Route = createFileRoute("/api/public/v1/sync/favorites" as any)({
  server: {
    handlers: {
      OPTIONS: async () => preflight(),
      GET: async ({ request, env }: any) => {
        const auth = await requireUser(request, env);
        if ("error" in auth) return auth.error;

        const db = getDB(env);
        const result = await db.prepare(
          "SELECT * FROM sync_favorites WHERE user_id = ? ORDER BY updated_at DESC"
        ).bind(auth.userId).all();

        return json({ favorites: result.results ?? [] });
      },
      POST: async ({ request, env }: any) => {
        const auth = await requireUser(request, env);
        if ("error" in auth) return auth.error;

        const body = await request.json().catch(() => null);
        if (!body?.media_id) return json({ error: "media_id_required" }, 400);

        const db = getDB(env);
        const now = new Date().toISOString();

        await db.prepare(
          "INSERT OR REPLACE INTO sync_favorites (user_id, media_id, media_type, title, poster_url, updated_at) VALUES (?, ?, ?, ?, ?, ?)"
        ).bind(auth.userId, body.media_id, body.media_type || "anime", body.title || null, body.poster_url || null, now).run();

        return json({ success: true });
      },
      DELETE: async ({ request, env }: any) => {
        const auth = await requireUser(request, env);
        if ("error" in auth) return auth.error;

        const url = new URL(request.url);
        const mediaId = url.searchParams.get("media_id");
        if (!mediaId) return json({ error: "media_id_required" }, 400);

        const db = getDB(env);
        await db.prepare("DELETE FROM sync_favorites WHERE user_id = ? AND media_id = ?").bind(auth.userId, mediaId).run();

        return json({ success: true });
      },
    },
  },
});
