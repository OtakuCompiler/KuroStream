import { createFileRoute } from "@tanstack/react-router";
import { json, preflight, requireUser, getDB } from "@/lib/kuro-api";

export const Route = createFileRoute("/api/public/v1/sync/profiles" as any)({
  server: {
    handlers: {
      OPTIONS: async () => preflight(),
      GET: async ({ request, env }: any) => {
        const auth = await requireUser(request, env);
        if ("error" in auth) return auth.error;

        const db = getDB(env);
        const result = await db.prepare(
          "SELECT * FROM sync_profiles WHERE user_id = ? ORDER BY created_at ASC"
        ).bind(auth.userId).all();

        const profiles = (result.results ?? []).map((row: any) => ({
          ...row,
          settings: row.settings ? JSON.parse(row.settings) : null,
        }));

        return json({ profiles });
      },
      POST: async ({ request, env }: any) => {
        const auth = await requireUser(request, env);
        if ("error" in auth) return auth.error;

        const body = await request.json().catch(() => null);
        if (!body?.profile_id || !body?.name) {
          return json({ error: "profile_id and name required" }, 400);
        }

        const db = getDB(env);
        const now = new Date().toISOString();

        await db.prepare(
          "INSERT OR REPLACE INTO sync_profiles (user_id, profile_id, name, avatar_url, is_kids, settings, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?)"
        ).bind(
          auth.userId,
          body.profile_id,
          body.name,
          body.avatar_url || null,
          body.is_kids ? 1 : 0,
          body.settings ? JSON.stringify(body.settings) : null,
          now
        ).run();

        return json({ success: true });
      },
      DELETE: async ({ request, env }: any) => {
        const auth = await requireUser(request, env);
        if ("error" in auth) return auth.error;

        const url = new URL(request.url);
        const profileId = url.searchParams.get("profile_id");
        if (!profileId) return json({ error: "profile_id_required" }, 400);

        const db = getDB(env);
        await db.prepare("DELETE FROM sync_profiles WHERE user_id = ? AND profile_id = ?").bind(auth.userId, profileId).run();

        return json({ success: true });
      },
    },
  },
});
