import { createFileRoute } from "@tanstack/react-router";
import { json, preflight, requireUser, getDB } from "@/lib/kuro-api";

export const Route = createFileRoute("/api/public/v1/sync/settings")({
  server: {
    handlers: {
      OPTIONS: async () => preflight(),
      GET: async ({ request, env }: any) => {
        const auth = await requireUser(request, env);
        if ("error" in auth) return auth.error;

        const db = getDB(env);
        const result = await db.prepare(
          "SELECT key, value FROM sync_settings WHERE user_id = ?"
        ).bind(auth.userId).all();

        const settings: Record<string, string> = {};
        for (const row of result.results ?? []) {
          settings[row.key as string] = row.value as string;
        }

        return json({ settings });
      },
      PUT: async ({ request, env }: any) => {
        const auth = await requireUser(request, env);
        if ("error" in auth) return auth.error;

        const body = await request.json().catch(() => null);
        if (!body?.settings || typeof body.settings !== "object") {
          return json({ error: "settings_object_required" }, 400);
        }

        const db = getDB(env);
        const now = new Date().toISOString();

        for (const [key, value] of Object.entries(body.settings)) {
          await db.prepare(
            "INSERT OR REPLACE INTO sync_settings (user_id, key, value, updated_at) VALUES (?, ?, ?, ?)"
          ).bind(auth.userId, key, String(value), now).run();
        }

        return json({ success: true, count: Object.keys(body.settings).length });
      },
    },
  },
});
